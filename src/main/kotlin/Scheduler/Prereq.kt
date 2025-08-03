package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import org.jetbrains.exposed.v1.core.Table

object Prereq : Table("prereq"){
    val id = uuid("id")
    val parentCourse = reference("parent_course",Course.id)
    val parentPrereq = optReference("parent_prereq",Prereq.id)
    val type = varchar("type", DEFAULT_MAX_STR_LENGTH)

    override val primaryKey = PrimaryKey(id)
}

