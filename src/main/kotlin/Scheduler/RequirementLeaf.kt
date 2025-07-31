package J.dev.Scheduler

import org.jetbrains.exposed.v1.core.Table

object RequirementLeaf : Table("requirement_leaf"){
    val requirementId = reference("requirement_id",DegreeRequirement.requirementId)
    val courseId = reference("course_id",Course.id)


    override val primaryKey = PrimaryKey(requirementId, courseId)

}