package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoutePortExtensionsTest {

    @Test
    fun `onPort matches only specified connector port`() = testApplication {
        application {
            routing {
                onPort(8080) {
                    get("/only-8080") {
                        call.respondText("ok-8080")
                    }
                }
            }
        }

        val client = createClient { }
        val response = client.get("/only-8080")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("ok-8080", response.bodyAsText())
    }
}

