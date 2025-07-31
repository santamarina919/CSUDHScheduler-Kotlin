package J.dev.Scheduler

import J.dev.DEFAULT_MAX_STR_LENGTH
import J.dev.Individuals.Individual
import J.dev.UUIDSerializer
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.Table
import java.util.*

object DegreePlan : Table("degree_plan") {

    object QueryNames{
        val planId = "planId"
    }


    @Serializable(with = UUIDSerializer::class)
    val id = uuid("id").default(UUID.randomUUID())
    val name = varchar("name", length = DEFAULT_MAX_STR_LENGTH)
    val term = varchar("term", length = 15)
    val year = integer("year")
    val majorId = reference("major_id",Degree.id)
    val planOwner = reference("plan_owner",Individual.email)

    override val primaryKey = PrimaryKey(id)
}