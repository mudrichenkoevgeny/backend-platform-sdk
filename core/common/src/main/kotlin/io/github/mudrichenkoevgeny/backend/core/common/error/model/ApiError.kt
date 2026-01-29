package io.github.mudrichenkoevgeny.backend.core.common.error.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val id: String,
    val code: String,
    val message: String,
    val args: Map<String, String> = emptyMap()
)