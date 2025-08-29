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
            prerequisiteOfCoursesFromDegree()
            degreeRequirementRoots()
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

fun Route.degreeRequirementRoots(){
    get("/api/degree/roots"){
        val degreeId = call.queryParameters[Degree.CONSTANTS.QUERY_ID]
            ?: throw IllegalStateException("No degree id present in query")

        val roots = rootsOfDegree(degreeId)
        call.respond(roots)
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

    get("api/degree/requirements"){
        val degreeId = call.queryParameters[Degree.CONSTANTS.QUERY_ID]
            ?: throw IllegalStateException("degree id cannot be null")

        val requirements = fetchNestedRequirementDetails(degreeId)
        call.respond(requirements)
    }
}

fun Route.coursesOfDegree() {

    get("api/degree/courses"){
        val degreeId = call.queryParameters[Degree.CONSTANTS.QUERY_ID]
        if(degreeId != null){

            val courses = coursesFrom(degreeId)
            call.respond(courses)

        }
        else{
            call.respond(HttpStatusCode.BadRequest)
        }

    }
}

fun Route.prerequisiteOfCoursesFromDegree(){
    get("/api/degree/prerequisites"){
        val degreeId = call.queryParameters[Degree.CONSTANTS.QUERY_ID]
        if(degreeId != null){
            val nestedPrerequisites = prerequisiteDetailsForDegree(degreeId)
            call.respond(nestedPrerequisites)
        }
        else
        {
            call.respond(HttpStatusCode.BadRequest)
        }

    }
}



