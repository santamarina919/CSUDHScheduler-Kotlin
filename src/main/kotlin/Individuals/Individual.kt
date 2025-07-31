package J.dev.Individuals

import org.jetbrains.exposed.v1.core.Table

val MAX_LENGTH = 100

object Individual : Table("individual") {
    val email = varchar("email", MAX_LENGTH)
    val password = varchar("password", MAX_LENGTH)
    val firstName = varchar("first_name", MAX_LENGTH)
}