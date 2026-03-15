package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.route

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.config.SwaggerConfig
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.routing.Route

/**
 * Registers Swagger UI and OpenAPI JSON endpoints on this [Route].
 *
 * - Serves the OpenAPI spec at [SwaggerConfig.OPENAPI_JSON_PATH] (e.g. `/api.json`).
 * - Serves Swagger UI at [SwaggerConfig.SWAGGER_UI_PATH] (e.g. `/swagger`), which loads the spec from the path above.
 *
 * Call this from the application's routing block (e.g. only when not in production) so that
 * the API documentation is available on the same host.
 */
fun Route.setupSwaggerEndpoints() {
    route("/${SwaggerConfig.OPENAPI_JSON_PATH}") {
        openApi()
    }
    route(SwaggerConfig.SWAGGER_UI_PATH) {
        swaggerUI(SwaggerConfig.OPENAPI_JSON_PATH)
    }
}