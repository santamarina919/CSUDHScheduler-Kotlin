package J.dev.Scheduler

import J.dev.UUIDSerializer
import io.ktor.server.http.content.CompressedFileType
import io.ktor.server.request.PipelineRequest
import io.ktor.server.request.RequestAlreadyConsumedException
import io.ktor.util.StatelessHmacNonceManager
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Serializable
data class DegreeDTO(val majorId :String, val majorName :String)


fun allMajors() =
    transaction {
        Degree.selectAll().toList()
            .map { DegreeDTO(it[Degree.id],it[Degree.name]) }
    }

@Serializable
data class DegreePlanDTO(@Serializable(with = UUIDSerializer::class) val id :UUID,
                         val name :String, val term : Term, val year :Int, val majorId :String, val owner :String)

fun allDegreePlansFrom(email :String) =
    transaction {
        DegreePlan.selectAll()
            .where(DegreePlan.planOwner eq email)
            .map { DegreePlanDTO(it[DegreePlan.id],it[DegreePlan.name],
                Term.valueOf(it[DegreePlan.term]),it[DegreePlan.year],it[DegreePlan.majorId],it[DegreePlan.planOwner]) }
    }

fun createPlan(name: String, term: Term, year: Int, majorId: String, email: String) =
    transaction {
        DegreePlan.insert {
            it[DegreePlan.name] = name
            it[DegreePlan.term] = term.name
            it[DegreePlan.year] = year
            it[DegreePlan.majorId] = majorId
            it[DegreePlan.planOwner] = email

        }
    } get DegreePlan.id

fun deletePlan(planId :UUID){
    transaction {
        Enrollment.deleteWhere {
            Enrollment.planId eq planId
        }

        DegreePlan.deleteWhere {
            DegreePlan.id eq planId
        }
    }
}

@Serializable
data class EnrollmentRecord(val courseId :String, val semester :Int)

fun allCompletedCoursesFrom(planId :UUID) = transaction {
    Enrollment.selectAll()
        .where(Enrollment.planId eq planId)
        .map { EnrollmentRecord(it[Enrollment.courseId],it[Enrollment.semester]) }
        .toList()
}

fun ownerOfDegreePlan(planId :UUID) = transaction {
        DegreePlan.select(DegreePlan.planOwner)
            .where(DegreePlan.id eq planId)
            .map { it[DegreePlan.planOwner] }
            .single()
    }

@Serializable
data class CourseDTO(val courseId :String, val courseName :String, val units :Double)

//TODO: Delete
fun coursesOfDegree(degreeId :String): List<CourseDTO> {
    val courseIds = requirementAndCoursesOfDegree(degreeId)
        .flatMap { it.requiredCourses }

    val courseDTOS = mutableListOf<CourseDTO>()

    courseIds.forEach{ idStr ->

        val courseData = transaction {
            Course.selectAll()
                .where(Course.id eq idStr)
                .single()
        }

        courseDTOS.add(CourseDTO(courseData[Course.id],courseData[Course.name],courseData[Course.units]))

    }
    return courseDTOS.toList()

}

fun getPrerequisite(courseId: String) = transaction {
    Prereq.selectAll()
        .where(Prereq.parentCourse eq courseId)
        .toList()
}

fun getPrerequisiteChildren(prereqId :UUID) = transaction {
    Prereq.selectAll()
        .where(Prereq.parentPrereq eq prereqId)
        .toList()
}

fun getLeafCoursesOfPrereq(prereqId: UUID) = transaction {
    LeafCourse.selectAll()
        .where(LeafCourse.prereqId eq prereqId)
        .toList()
}

fun allDegreeRequirements(degreeId: String) = transaction {
    DegreeRequirement.join(
        Degree,
        JoinType.INNER,
        additionalConstraint = {Degree.id eq DegreeRequirement.parentMajor}
    )
        .select(DegreeRequirement.requirementId,DegreeRequirement.parentRequirement,DegreeRequirement.type)
        .where(DegreeRequirement.parentMajor eq degreeId)
        .toList()
}

fun allDegreeRequirementsLeafCourses(requirementId :UUID) = transaction {
    RequirementLeaf.select(RequirementLeaf.requirementId,RequirementLeaf.courseId)
        .where(RequirementLeaf.requirementId eq requirementId)
        .toList()
}

@Serializable
data class RequirementNode(@Serializable(with = UUIDSerializer::class) val requirementId :UUID,
                           val type :RequirementType,
                           val children : MutableList<RequirementNode>,
                           val requiredCourses :MutableList<String>)

fun requirementAndCoursesOfDegree(degreeId :String): MutableCollection<RequirementNode> {
    val requirementMap = mutableMapOf<UUID,RequirementNode>()

    val requirements = allDegreeRequirements(degreeId)

    requirements.forEach {
        requirementMap[it[DegreeRequirement.requirementId]] =
            RequirementNode(
                it[DegreeRequirement.requirementId],
                RequirementType.valueOf(it[DegreeRequirement.type]),
                mutableListOf(),
                mutableListOf()
            )
    }

    requirements.forEach {
        val selfId = it[DegreeRequirement.requirementId]
        val parentId = it[DegreeRequirement.parentRequirement]
        if(parentId != null)
        requirementMap[parentId]!!.children.add(requirementMap[selfId]!!)
    }

    requirements.forEach { req ->
        val leafNodes = allDegreeRequirementsLeafCourses(req[DegreeRequirement.requirementId])
        leafNodes.forEach { leaf ->
            requirementMap[leaf[RequirementLeaf.requirementId]]!!.requiredCourses.add(leaf[RequirementLeaf.courseId])
        }
    }

    return requirementMap.values;
}

//TODO: delete
@Serializable
data class AvailableCourse(val courseId :String,val semesterAvailable :Int){}

/**
 * Returns a list of courses that can be added to a plan
 */
//TODO: DELETE
fun availableCoursesOfPlan(planId :UUID): MutableList<AvailableCourse> {
    val completed = allCompletedCoursesFrom(planId)

    val completedByCourseId = completed.associateBy { it.courseId }


    val uncompletedCourses = coursesOfDegree("CSC")
        .filter{ !completedByCourseId.contains(it.courseId) }
    val available = mutableListOf<AvailableCourse>()

    uncompletedCourses.forEach {
        val rootPrereq = getPrerequisite(it.courseId)
        println(rootPrereq)
        if(rootPrereq.isEmpty()){
            available.add(AvailableCourse(it.courseId,0))
        }
        else{
            val semesterAvail = isValidTree(rootPrereq.single(),completedByCourseId)
            if(semesterAvail != null){
                available.add(AvailableCourse(it.courseId,semesterAvail + 1))
            }
        }
    }
    return available

}

/**
 * Determines if a given enrollment record satisfies a non-null prerequisite tree.
 */
fun isValidTree(node: ResultRow, completedByCourseId: Map<String,EnrollmentRecord>) :Int? {
    val type = RequirementType.valueOf(node[Prereq.type])
    val leafCourses = getLeafCoursesOfPrereq(node[Prereq.id])
    val childrenPrerequisites = getPrerequisiteChildren(node[Prereq.id])

    var validNode = false

    var semesterAvail = -1

    //The type of prerequisite node determines which function is called. The function then applies a set of rules against the plan
    //to determine if it is satisfied
    when(type) {
        RequirementType.AND -> {
            var latestSemesterPCompleted = -1

            val validCourseCompletion = leafCourses.all {
                val enrollmentRecord = completedByCourseId[it[LeafCourse.courseId]]
                if(enrollmentRecord != null){
                    latestSemesterPCompleted = Math.max(latestSemesterPCompleted,enrollmentRecord.semester)
                    true
                }
                else{
                    false
                }
            }


            val validChildPrereqCompletion = childrenPrerequisites.all {
                val latestChildPCompleted = isValidTree(it,completedByCourseId)
                if(latestChildPCompleted != null){
                    latestSemesterPCompleted = Math.max(latestSemesterPCompleted,latestChildPCompleted)
                }
                latestChildPCompleted != null
            }


            validNode = validCourseCompletion && validChildPrereqCompletion
            semesterAvail = latestSemesterPCompleted
        }

        RequirementType.OR -> {
            var validCourseCompletion = false
            var earliestSemesterPCompleted = Int.MAX_VALUE
            leafCourses.forEach{
                val enrollmentRecord = completedByCourseId[it[Course.id]]
                if(enrollmentRecord != null){
                    earliestSemesterPCompleted = Math.min(earliestSemesterPCompleted,enrollmentRecord.semester)
                    validCourseCompletion = true
                }
            }


            val validChildPrereqCompletion = childrenPrerequisites.any{
                val earliestchildPCompleted = isValidTree(it,completedByCourseId)
                if(earliestchildPCompleted != null){
                    earliestSemesterPCompleted = Math.min(earliestSemesterPCompleted,earliestchildPCompleted)
                }
                earliestchildPCompleted != null
            }

            validNode = validCourseCompletion || validChildPrereqCompletion
            semesterAvail = earliestSemesterPCompleted
        }
    }


    if(!validNode){
        return null
    }


    return semesterAvail
}

/**
 * Function returns a list of course ids where they all list the course in function paramateras a prerequisite.
 */
fun coursesThatHavePreqreuisite(courseId :String): List<String> {
    val courses = mutableListOf<List<String>>()

    transaction {
        val prereqNodes = LeafCourse.select(LeafCourse.prereqId)
            .where(LeafCourse.courseId eq courseId)
            .toList()
         prereqNodes.forEach{node ->
            val nodeId = node[LeafCourse.prereqId]

             val currCourseList = transaction {
                Prereq.select(Prereq.parentCourse)
                    .where(Prereq.id eq nodeId)
                    .map { it[Prereq.parentCourse] }
            }

             courses.add(currCourseList)

         }
    }

    return courses.toList().flatten()



}

fun determineAvailability(affectedCourses: List<String>, planId :UUID): MutableList<AvailableCourse> {
    val availableCoursesQ = mutableListOf<AvailableCourse>()

    val coursesTaken = allCompletedCoursesFrom(planId).associateBy { it.courseId }

    affectedCourses.forEach { courseId ->
        val prereqs = getPrerequisite(courseId)

        val semesterAvailable = isValidTree(prereqs.single(),coursesTaken)

        if(semesterAvailable != null){
            val newlyAvailable = transaction {
                Course.selectAll()
                    .where(Course.id eq courseId)
                    .map { AvailableCourse(it[Course.id],semesterAvailable + 1) }
                    .single()
            }
            availableCoursesQ.add(newlyAvailable)
        }
    }


    return availableCoursesQ
}





fun addCourseToPlan(planId : UUID, courseId: String, semester: Int) : MutableList<AvailableCourse> {
    transaction {
        Enrollment.insert {
            it[Enrollment.planId] = planId
            it[Enrollment.courseId] = courseId
            it[Enrollment.semester] = semester
        }
    }

    val affectedCourses = coursesThatHavePreqreuisite(courseId)

    val newlyAvailableCourse = determineAvailability(affectedCourses,planId)

    return newlyAvailableCourse
}

fun removeCourseFromPlan(planId :UUID, courseId :String,cascadeRemoveApproved : Boolean): List<String> {
    val completedCourses = allCompletedCoursesFrom(planId)
        .associateBy { it.courseId }

    val affectedCourses = coursesThatHavePreqreuisite(courseId)
        .filter { completedCourses.contains(it) }
        .toMutableList()

    affectedCourses.add(courseId)

    if(cascadeRemoveApproved) {

        affectedCourses.forEach { idStr ->
            transaction {
                Enrollment.deleteWhere {
                    (Enrollment.planId eq planId) and (Enrollment.courseId eq idStr)
                }
            }
        }
    }
    return affectedCourses
}

/**
 * Class is a mapping using simple data types of the course table
 */
@Serializable
data class CourseDetails(val id :String, val name : String, val units : Double)

/**
 * Function returns all courses that are related to a specific degree.
 * Related in this context means that some requirement of that degree
 * requires a course in the list that is returned
 */
fun coursesFrom(degreeId : String) :List<CourseDetails> = transaction {

        RequirementLeaf.join(
            otherTable = DegreeRequirement,
            joinType = JoinType.INNER,
            onColumn = RequirementLeaf.requirementId,
            otherColumn = DegreeRequirement.requirementId
        )
        .join(
            otherTable = Course,
            joinType = JoinType.INNER,
            onColumn = Course.id,
            otherColumn = RequirementLeaf.courseId
        )
            .select(Course.id,Course.name,Course.units)
            .where { DegreeRequirement.parentMajor eq degreeId }
            .map {resultRow -> CourseDetails(resultRow[Course.id],resultRow[Course.name],resultRow[Course.units]) }

}


@Serializable
data class NestedRequirementDetails(
    @Serializable(with = UUIDSerializer::class) val requirementId: UUID,
    val name : String?,
    val type : RequirementType,
    val childRequirements :List<@Serializable(with = UUIDSerializer::class) UUID>,
    val leafCourses :List<String>
)

//TODO: add name column to table
data class RequirementDetails(val requirementId : UUID,val type : RequirementType)

private fun requirementsFrom(degreeId: String) :List<RequirementDetails> = transaction {
        DegreeRequirement
            .select(DegreeRequirement.requirementId, DegreeRequirement.type)
            .where { DegreeRequirement.parentMajor eq degreeId }
            .map { resultRow ->
                RequirementDetails(
                    resultRow[DegreeRequirement.requirementId],
                    RequirementType.valueOf(resultRow[DegreeRequirement.type])
                )
            }
    }

//TODO: validate function returns whaat is expected
private fun childrenOfRequirements(requirementId: UUID) = transaction {
        val degreeRequirementAlias = DegreeRequirement.alias("degreeRequirementAlias")
        DegreeRequirement
            .join(
                otherTable = degreeRequirementAlias,
                joinType = JoinType.INNER,
                onColumn = DegreeRequirement.requirementId,
                otherColumn = degreeRequirementAlias[DegreeRequirement.parentRequirement]
            )
            .select(DegreeRequirement.requirementId)
            .where { DegreeRequirement.requirementId eq requirementId }
            .map { resultRow -> resultRow[DegreeRequirement.requirementId] }
    }


private fun leafCoursesOfRequirement(requirementId: UUID) = transaction {
        RequirementLeaf
            .select(RequirementLeaf.courseId)
            .where { RequirementLeaf.requirementId eq requirementId}
            .map { resultRow -> resultRow[RequirementLeaf.courseId] }

    }


fun fetchNestedRequirementDetails(degreeId : String): MutableList<NestedRequirementDetails> {
    val requirementList = mutableListOf<NestedRequirementDetails>()
    val requirements = requirementsFrom(degreeId)
    requirements.forEach { requirement ->
        val children = childrenOfRequirements(requirement.requirementId)
        val leafCourses = leafCoursesOfRequirement(requirement.requirementId)
        requirementList.add(NestedRequirementDetails(requirement.requirementId,null,requirement.type,  children,leafCourses))
    }
    return requirementList
}

@Serializable
data class NestedPrerequisiteDetails(
        @Serializable(with = UUIDSerializer::class) val prereqId : UUID,
        val parentCourse : String,
        val type : RequirementType,
        val childrenPrereqs :List<@Serializable(with = UUIDSerializer::class) UUID>?,
        val leafCourses :List<String>
    )


fun prerequisiteDetailsForDegree(degreeId: String): MutableList<List<NestedPrerequisiteDetails>> {
    val courseList = coursesFrom(degreeId)
    val returnlist = mutableListOf<List<NestedPrerequisiteDetails>>()
    println(courseList)
    courseList.forEach { course ->
        val prereqList = fetchNestedPrerequisiteDetails(course.id)
        prereqList?.let {
            returnlist.add(it)
        }
    }
    return returnlist
}

data class PrereqDTO(val id : UUID, val parentCourse : String, val parentPrereq : UUID?, val requirementType: RequirementType)

fun fetchNestedPrerequisiteDetails(courseId: String) :List<NestedPrerequisiteDetails>? {
    lateinit var prereqs :List<PrereqDTO>

    //Maps a requirement id to a list of courses that that requirement needs
    val leafCourseMap = mutableMapOf<UUID, MutableList<String>>()
    val childPrereqMap = mutableMapOf<UUID, MutableList<UUID>>()

    transaction {
        prereqs = Prereq.selectAll()
            .where { Prereq.parentCourse eq courseId }
            .map { resultRow ->
                PrereqDTO(
                    resultRow[Prereq.id],
                    resultRow[Prereq.parentCourse],
                    resultRow[Prereq.parentPrereq],
                    RequirementType.valueOf(resultRow[Prereq.type]))
            }

        prereqs.forEach { prereq ->
            val ids = LeafCourse.select(LeafCourse.courseId)
                .where { LeafCourse.prereqId eq prereq.id }
                .map { resultRow -> resultRow[LeafCourse.courseId] }

            ids.forEach { id ->
                leafCourseMap.putIfAbsent(prereq.id,mutableListOf<String>())
                leafCourseMap[prereq.id]?.add(id)
            }

            prereq.parentPrereq?.also { parentId ->
                childPrereqMap.putIfAbsent(parentId,mutableListOf())
                childPrereqMap[parentId]?.add(parentId)
            }


        }
    }

    return prereqs.map { prereq ->

        NestedPrerequisiteDetails(
            prereq.id,
            prereq.parentCourse,
            prereq.requirementType,
            childPrereqMap[prereq.id],
            leafCourseMap[prereq.id]!!)
    }
        .takeIf { it.isNotEmpty() }
}





