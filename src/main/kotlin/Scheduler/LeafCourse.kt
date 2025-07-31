package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import org.jetbrains.exposed.v1.core.Table

object LeafCourse : Table("leaf_course") {
    val courseId = varchar("course_id", DEFAULT_MAX_STR_LENGTH)
    val prereqId = uuid("prereq_id")
    val concurrent = bool("concurrent")

}