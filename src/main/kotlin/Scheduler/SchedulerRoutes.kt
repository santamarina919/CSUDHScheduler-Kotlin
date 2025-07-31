package J.dev.Scheduler

import J.dev.Individuals.IndividualSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.lang.IllegalStateException

fun Application.SchedulerRoutes(){
    routing {

        authenticate ("auth-session") {
            allMajorsEndPoint()
            createDegreePlanEndPoint()
            allDegreePlanEndPoint()
            requirementsOfDegree()
            coursesOfDegree()
        }
    }

}


fun Route.allMajorsEndPoint(){

    get("/api/major/all") {
        call.respond(allMajors())
    }
}

fun Route.allDegreePlanEndPoint() {
    get("/api/degree/all"){
        val email = call.principal<IndividualSession>()?.username
            ?: throw IllegalStateException("Call to principal returned null in create degree plan end point")

        val plans = allDegreePlansFrom(email)
        call.respond(HttpStatusCode.OK,plans)
    }
}



fun Route.createDegreePlanEndPoint(){

    @Serializable
    data class CreateBody(val name :String, val term : Term, val year :Int, val majorId :String)


    post("/api/degree/create"){
        val planData = call.receive<CreateBody>()
        val email = call.principal<IndividualSession>()?.username
            ?: throw IllegalStateException("Call to principal returned null in create degree plan end point")


        val degreeId = createPlan(planData.name,planData.term,planData.year,planData.majorId,email)

        call.respond(HttpStatusCode.Created, DegreePlanDTO(degreeId,planData.name,planData.term,planData.year,planData.majorId,email))
    }

}




fun Route.requirementsOfDegree(){

    @Serializable
    data class EndPointBody(val degreeId :String)

    get("api/degree/requirements"){
        val body = call.receive<EndPointBody>()

        val courses = requirementAndCoursesOfDegree(body.degreeId)
        call.respond(courses )
    }
}

fun Route.coursesOfDegree() {

    get("api/degree/courses"){
        val degreeId = call.queryParameters["degreeId"]
        if(degreeId != null){

            val courses = coursesOfDegree(degreeId)
            call.respond(courses)

        }
        else{
            call.respond(HttpStatusCode.BadRequest)
        }

    }
}





