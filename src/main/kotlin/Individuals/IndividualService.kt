package J.dev.Individuals

import J.dev.Individuals.Individual.email
import J.dev.Individuals.Individual.firstName
import at.favre.lib.crypto.bcrypt.BCrypt
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


fun signUpIndividual(individualEmail :String, pass :String, name :String): Boolean {

    val hashedPassword = BCrypt.withDefaults().hashToString(12,pass.toCharArray())
    val dupe = transaction {

        try {
            Individual.insert {
                it[email] = individualEmail
                it[password] = hashedPassword
                it[firstName] = name
            } get firstName
        } catch (error: ExposedSQLException) {
            return@transaction false
        }
        return@transaction true
    }
    return dupe
}

fun validLogin(emailId :String, password :String) :Boolean{
    val actualPW = transaction {
        Individual.select(Individual.password)
            .where {email eq emailId}
            .first()[Individual.password]
    }


    return BCrypt.verifyer().verify(password.toByteArray(),actualPW.toByteArray()).verified
}