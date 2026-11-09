package io.github.mudrichenkoevgeny.backend.core.common.routing

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.RequestConnectionPoint
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoutePortUtilsTest {
    private val testPortPlugin = createApplicationPlugin("TestPortPlugin") {
        onCall { call ->
            val mockedPort = call.request.headers["X-Test-Port"]?.toIntOrNull()
            if (mockedPort != null) {
                val originalConnectionPoint = call.request.local
                val mockedConnectionPoint = object : RequestConnectionPoint by originalConnectionPoint {
                    @Suppress("OVERRIDE_DEPRECATION")
                    override val port: Int = mockedPort
                    override val localPort: Int = mockedPort
                    override val serverPort: Int = mockedPort
                }

                val connectionPointField = call.request::class.java.declaredFields.firstOrNull {
                    it.name == "_local" || it.type == RequestConnectionPoint::class.java
                }

                connectionPointField?.apply {
                    isAccessible = true
                    set(
                        call.request,
                        mockedConnectionPoint
                    )
                }
            }
        }
    }

    @Test
    fun `onPort matches only specified connector port`() = testApplication {
        application {
            install(testPortPlugin)
            routing {
                onPort(8080) {
                    get(
                        path = TEST_ROUTE_PATH,
                        body = { handleTest8080() }
                    )
                }
                onPort(9090) {
                    get(
                        path = TEST_ROUTE_PATH,
                        body = { handleTest9090() }
                    )
                }
            }
        }

        val response8080 = client.get(
            urlString = TEST_ROUTE_PATH,
            block = {
                header(
                    "X-Test-Port",
                    "8080"
                )
            }
        )
        assertEquals(
            HttpStatusCode.OK,
            response8080.status
        )
        assertEquals(
            "ok-8080",
            response8080.bodyAsText()
        )

        val response9090 = client.get(
            urlString = TEST_ROUTE_PATH,
            block = {
                header(
                    "X-Test-Port",
                    "9090"
                )
            }
        )
        assertEquals(
            HttpStatusCode.OK,
            response9090.status
        )
        assertEquals(
            "ok-9090",
            response9090.bodyAsText()
        )

        val response404 = client.get(
            urlString = TEST_ROUTE_PATH,
            block = {
                header(
                    "X-Test-Port",
                    "7070"
                )
            }
        )
        assertEquals(
            HttpStatusCode.NotFound,
            response404.status
        )
    }

    private suspend fun RoutingContext.handleTest8080() {
        call.respondText("ok-8080")
    }

    private suspend fun RoutingContext.handleTest9090() {
        call.respondText("ok-9090")
    }

    private companion object {
        private const val TEST_ROUTE_PATH = "/test"
    }
}
