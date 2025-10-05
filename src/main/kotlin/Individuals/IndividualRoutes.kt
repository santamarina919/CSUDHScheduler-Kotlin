package J.dev.Individuals

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

fun Application.IndividualRoutes() {
    routing {
        signUp()
        logIn()
        testEndpoint()

    }

}

fun Route.testEndpoint() {
    get("/api/test") {
        call.respondText("Response from aws")
    }
}

fun Route.signUp() {

    @Serializable
    data class SignUpData(
        val email: String,
        val password: String,
        val firstName: String
    )

    post("/api/individual/signup") {
        val requestData = call.receive<SignUpData>()
        val success = signUpIndividual(requestData.email,requestData.password,requestData.firstName)

        call.respond(if (success) HttpStatusCode.Created else HttpStatusCode.Conflict)
    }
}

fun Route.logIn() {

    @Serializable
    data class LoginData(
        val email :String,
        val password :String,
    )

    post("/api/individual/login") {
        val loginData = call.receive<LoginData>()
        val success = validLogin(loginData.email,loginData.password)
        call.sessions.set(IndividualSession(loginData.email, 0))
        println(success)
        call.respond(if(success) HttpStatusCode.Accepted else HttpStatusCode.Unauthorized)
    }
}