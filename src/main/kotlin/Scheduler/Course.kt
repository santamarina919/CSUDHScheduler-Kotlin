package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import org.jetbrains.exposed.v1.core.Table

object Course : Table("course") {
    val id = varchar("id", DEFAULT_MAX_STR_LENGTH)
    val name = varchar("name", DEFAULT_MAX_STR_LENGTH)
    val units = double("units")
}