package io.github.mudrichenkoevgeny.backend.core.common.config.swagger.model

data class SwaggerConfig(
    val title: String,
    val description: String,
    val version: String,
    val serverUrl: String,
    val serverDescription: String
)