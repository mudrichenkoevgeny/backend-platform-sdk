package io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model

/**
 * Configuration used to expose service metadata in Swagger / OpenAPI documentation.
 *
 * @param title human-readable API title.
 * @param description short description of the API.
 * @param version semantic version of the service.
 * @param serverUrl base URL of the server where the API is hosted.
 * @param serverDescription human-readable name of the server (e.g. environment label).
 */
data class SwaggerConfig(
    val title: String,
    val description: String,
    val version: String,
    val serverUrl: String,
    val serverDescription: String
)