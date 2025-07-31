package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import org.jetbrains.exposed.v1.core.Table

object DegreeRequirement : Table("degree_requirement") {
    val requirementId = uuid("requirement_id")
    val type = varchar("type", DEFAULT_MAX_STR_LENGTH)

    val parentMajor = reference("parent_major",Degree.id)
    val parentRequirement = reference("parent_requirement", requirementId).nullable()

    override val primaryKey = PrimaryKey(requirementId)
}