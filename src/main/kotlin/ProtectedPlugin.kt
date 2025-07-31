package J.dev

import J.dev.Individuals.IndividualSession
import J.dev.Scheduler.DegreePlan
import J.dev.Scheduler.ownerOfDegreePlan
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.sessions.*
import io.ktor.util.logging.*
import java.lang.IllegalArgumentException
import java.util.*


internal val LOGGER = KtorSimpleLogger("dev.J.ProtectedPlugin")

/**
 * The protected plugin ensures that a request sent to a given endpoint
 * for a specific resource is being made by only a person that has the permission to request it
 */
val ProtectedPlugin = createRouteScopedPlugin("ProtectedPlugin"){
    onCall { call  ->
        val sessionInfo = call.sessions.get<IndividualSession>()

        if(sessionInfo == null){
            LOGGER.atError().log("Request made but user is not logged in")
            call.respond(HttpStatusCode.Unauthorized,null)
            return@onCall
        }

        val params = call.request.queryParameters

        if(params.contains(DegreePlan.QueryNames.planId)){
            if(!validPermission(params[DegreePlan.QueryNames.planId]!!,sessionInfo.username)){
                call.respond(HttpStatusCode.Unauthorized,null)
                return@onCall
            }
        }


    }
}

fun validPermission(planIdStr :String, username :String): Boolean {
    var planId :UUID? = null
    try {
        planId = UUID.fromString(planIdStr)
    }
    catch (e :IllegalArgumentException){
        LOGGER.atError().log("UUID was not valid")
        return false
    }
    val planOwner = ownerOfDegreePlan(planId)

    if(planOwner != username){
        LOGGER.atError().log("""Request does not have valid permissions 
            (Plan owner ($planOwner) != session owner ($username))
        """.trimMargin())
        return false
    }
    else{
        LOGGER.atInfo().log("Request has valid permission for plan resource")
    }
    return true
}
