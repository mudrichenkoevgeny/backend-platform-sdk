package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.config

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.route.setupSwaggerEndpoints

/**
 * Path constants for Swagger UI and OpenAPI JSON in the documentation routes.
 *
 * Use [SWAGGER_UI_PATH] and [OPENAPI_JSON_PATH] when registering Swagger endpoints (e.g. in
 * [setupSwaggerEndpoints])
 * so that UI and spec are served at a consistent path.
 */
public object SwaggerConfig {
    /** URL path segment for the Swagger UI (e.g. `/swagger`). */
    public const val SWAGGER_UI_PATH: String = "swagger"

    /** Path to the OpenAPI JSON spec (e.g. `/api.json`); used by Swagger UI to load the spec. */
    public const val OPENAPI_JSON_PATH: String = "/api.json"
}
