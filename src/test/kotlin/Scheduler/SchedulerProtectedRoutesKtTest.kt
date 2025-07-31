package Scheduler

import io.ktor.client.request.*
import io.ktor.server.testing.*
import kotlin.test.Test

class SchedulerProtectedRoutesKtTest {

    @Test
    fun testGetProtectedApiDegreeplanPlanned() = testApplication {
        application {
            TODO("Add the Ktor module for the test")
        }
        client.get("/protected/*/api/degreeplan/planned").apply {
            TODO("Please write your test here")
        }
    }

    @Test
    fun testPostProtectedApiDegreeplanRemove() = testApplication {
        application {
            TODO("Add the Ktor module for the test")
        }
        client.post("/protected/api/degreeplan/remove").apply {
            TODO("Please write your test here")
        }
    }
}