package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.route

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.constants.SwaggerConstants
import io.github.smiley4.ktoropenapi.openApi
import io.github.smiley4.ktoropenapi.route
import io.github.smiley4.ktorswaggerui.swaggerUI
import io.ktor.server.routing.Route

fun Route.setupSwaggerEndpoints() {
    route("/${SwaggerConstants.OPENAPI_JSON_PATH}") {
        openApi()
    }
    route(SwaggerConstants.SWAGGER_UI_PATH) {
        swaggerUI(SwaggerConstants.OPENAPI_JSON_PATH)
    }
}