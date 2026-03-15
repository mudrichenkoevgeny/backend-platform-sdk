package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.route

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.config.SwaggerConfig
import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.config.OutputFormat
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for [setupSwaggerEndpoints]: ensures routes are registered and Swagger UI is reachable.
 * OpenAPI JSON at [SwaggerConfig.OPENAPI_JSON_PATH] is not asserted here because the plugin
 * requires a spec named "api" generated from OpenAPI-annotated routes, which are not present in this module.
 */
class SwaggerRouteTest {

    @Test
    fun `setupSwaggerEndpoints registers routes without throwing`() = testApplication {
        application {
            install(OpenApi) {
                info { title = "Test"; version = "1.0" }
                server { url = "http://localhost" }
                outputFormat = OutputFormat.JSON
            }
            routing {
                setupSwaggerEndpoints()
            }
        }
    }

    @Test
    fun `setupSwaggerEndpoints serves Swagger UI at SWAGGER_UI_PATH`() = testApplication {
        application {
            install(OpenApi) {
                info { title = "Test"; version = "1.0" }
                server { url = "http://localhost" }
                outputFormat = OutputFormat.JSON
            }
            routing {
                setupSwaggerEndpoints()
            }
        }
        val response = client.get("/${SwaggerConfig.SWAGGER_UI_PATH}")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
