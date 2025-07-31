package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import org.jetbrains.exposed.v1.core.Table

object Enrollment : Table("enrollment"){
    val courseId = reference("course_id",Course.id)
    val planId = reference("plan_id",DegreePlan.id)
    val semester = integer("semester")
}