package J.dev

import J.dev.Individuals.IndividualRoutes
import J.dev.Individuals.IndividualSession
import J.dev.Scheduler.SchedulerProtectedRoutes
import J.dev.Scheduler.SchedulerRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlin.collections.set



fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)


fun Application.module() {

    //Plugin that allows me to handle json in all routes
    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        session<IndividualSession>("auth-session") {
            validate {session ->
                session
            }
            challenge{
                call.respondText("",ContentType.Application.Json,HttpStatusCode.Unauthorized)
            }
        }
    }
    val ONEDAY = 86400L
    install(Sessions) {
        cookie<IndividualSession>("individual_session") {
            cookie.extensions["SameSite"] = "lax"
            cookie.path = "/"
            cookie.maxAgeInSeconds = ONEDAY
        }
    }


    //Initialize db settings
    DatabaseSettings.db

    //All routes for each feature are in the following functions
    IndividualRoutes()



    SchedulerRoutes()
    SchedulerProtectedRoutes()

    //Other configuration
    configureHTTP()

}
