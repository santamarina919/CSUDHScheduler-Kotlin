package J.dev.Scheduler

import J.dev.Individuals.IndividualSession
import J.dev.ProtectedPlugin
import J.dev.UUIDSerializer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.serialization.Serializable
import java.util.*

internal val LOGGER = KtorSimpleLogger("dev.J.main.Scheduler.SchedulerProtectedRoutes")

fun Application.SchedulerProtectedRoutes(){
    routing{

        route("/protected"){

            install(ProtectedPlugin)

            authenticate("auth-session") {

                allPlannedCoursesEndPoint()
                addCourseToPlanEndPoint()
                removeCourseFromPlanEndPoint()
                deleteDegreePlanEndPoint()

            }


        }

    }.run {
        val routes = getAllRoutes()
        LOGGER.atInfo().log("Protected routes: $routes")
    }
}


fun Route.allPlannedCoursesEndPoint(){



    get("/api/degree/planned"){


        val planId = call.request.queryParameters[DegreePlan.QueryNames.planId]
        val courses = allCompletedCoursesFrom(UUID.fromString(planId))
        println(courses)
        call.respond(HttpStatusCode.OK, courses)
    }

}

fun Route.deleteDegreePlanEndPoint(){

    delete("api/degree/delete"){
        val planIdAsStr = call.request.queryParameters[DegreePlan.QueryNames.planId] ?: "Plan Id cannot be null"
        val planId = UUID.fromString(planIdAsStr)
        deletePlan(planId)
        call.respond(HttpStatusCode.OK)
    }

}


fun Route.addCourseToPlanEndPoint(){

    @Serializable
    data class AddBody(val courseId :String, val semester :Int)

    post("/api/degree/add"){
        val planId = UUID.fromString(call.request.queryParameters[DegreePlan.QueryNames.planId])

        val body = call.receive<AddBody>()
        addCourseToPlan(planId,body.courseId,body.semester)
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.removeCourseFromPlanEndPoint(){
    @Serializable
    data class RemoveBody(val courseId :String,val removeApproved :Boolean)

    post("/api/degree/remove"){
        val planId = UUID.fromString(call.request.queryParameters[DegreePlan.QueryNames.planId])

        val body = call.receive<RemoveBody>()
        val response = removeCourseFromPlan(planId,body.courseId, body.removeApproved)

        call.respond(response)
    }
}




