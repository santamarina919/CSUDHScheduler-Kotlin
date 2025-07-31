package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import org.jetbrains.exposed.v1.core.Table

object Degree : Table("degree"){
    val id = varchar("id", length = 10)
    val name = varchar(name = "name", length = DEFAULT_MAX_STR_LENGTH)

    override val primaryKey = PrimaryKey(id)

}