package J.dev.Scheduler

import J.dev.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
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
