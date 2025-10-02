package J.dev.Scheduler

import J.dev.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.JoinType
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

fun rootsOfDegree(degreeId: String) =
    transaction {
        DegreeRequirement
            .select(DegreeRequirement.requirementId)
            .where { (DegreeRequirement.parentMajor eq degreeId) and (DegreeRequirement.parentRequirement eq null) }
            .map { resultRow -> resultRow[DegreeRequirement.requirementId].toString() }
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
data class PlanDetails(@Serializable(with = UUIDSerializer::class) val id : UUID, val name : String, val term : Term, val year : Int, val degreeId: String)

fun fetchPlanDetails(planId : UUID) = transaction {
    DegreePlan.selectAll()
        .where { DegreePlan.id eq planId }
        .map { resultRow ->
            PlanDetails(
                resultRow[DegreePlan.id],
                resultRow[DegreePlan.name],
                Term.valueOf(resultRow[DegreePlan.term]),
                resultRow[DegreePlan.year],
                resultRow[DegreePlan.majorId]
            )
        }
        .first()
}


@Serializable
data class RequirementNode(@Serializable(with = UUIDSerializer::class) val requirementId :UUID,
                           val type :RequirementType,
                           val children : MutableList<RequirementNode>,
                           val requiredCourses :MutableList<String>)


fun addCourseToPlan(planId : UUID, courseId: String, semester: Int) =
    transaction {
        Enrollment.insert {
            it[Enrollment.planId] = planId
            it[Enrollment.courseId] = courseId
            it[Enrollment.semester] = semester
        }
    }

fun removeCourseFromPlan(planId :UUID, courseId :String,cascadeRemoveApproved : Boolean) {

    transaction {
            Enrollment.deleteWhere {
                (Enrollment.planId eq planId) and (Enrollment.courseId eq courseId)
            }

        }
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

private fun childrenOfRequirements(requirementId: UUID) = transaction {
        val degreeRequirementAlias = DegreeRequirement.alias("degreeRequirementAlias")
        DegreeRequirement
            .join(
                otherTable = degreeRequirementAlias,
                joinType = JoinType.INNER,
                onColumn = DegreeRequirement.requirementId,
                otherColumn = degreeRequirementAlias[DegreeRequirement.parentRequirement]
            )
            .selectAll()
            .where { DegreeRequirement.requirementId eq requirementId }
            .map { resultRow -> resultRow[degreeRequirementAlias[DegreeRequirement.requirementId]] }
    }


private fun leafCoursesOfRequirement(requirementId: UUID) = transaction {
        RequirementLeaf
            .select(RequirementLeaf.courseId)
            .where { RequirementLeaf.requirementId eq requirementId}
            .map { resultRow -> resultRow[RequirementLeaf.courseId] }

    }


@Serializable
data class NestedPrerequisiteDetails(
        @Serializable(with = UUIDSerializer::class) val prereqId : UUID,
        @Serializable(with = UUIDSerializer::class) val parentPrereq : UUID?,
        val parentCourse : String,
        val type : RequirementType,
        val childrenPrereqs :List<@Serializable(with = UUIDSerializer::class) UUID>?,
        val leafCourses :List<String>
    )

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


fun prerequisiteDetailsForDegree(degreeId: String): List<NestedPrerequisiteDetails> {
    val courseList = coursesFrom(degreeId)
    val returnlist = mutableListOf<List<NestedPrerequisiteDetails>>()
    println(courseList)
    courseList.forEach { course ->
        val prereqList = fetchNestedPrerequisiteDetails(course.id)
        prereqList?.let {
            returnlist.add(it)
        }
    }
    return returnlist.flatten()
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
            childPrereqMap.putIfAbsent(prereq.id,mutableListOf())
            val ids = LeafCourse.select(LeafCourse.courseId)
                .where { LeafCourse.prereqId eq prereq.id }
                .map { resultRow -> resultRow[LeafCourse.courseId] }

            ids.forEach { id ->
                leafCourseMap.putIfAbsent(prereq.id,mutableListOf<String>())
                leafCourseMap[prereq.id]?.add(id)
            }

            prereq.parentPrereq?.also { parentId ->
                childPrereqMap[parentId]?.add(prereq.id)
            }


        }
    }

    return prereqs.map { prereq ->

        NestedPrerequisiteDetails(
            prereq.id,
            prereq.parentPrereq,
            prereq.parentCourse,
            prereq.requirementType,
            childPrereqMap[prereq.id],
            leafCourseMap[prereq.id]!!)
    }
        .takeIf { it.isNotEmpty() }
}





