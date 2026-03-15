package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import kotlinx.serialization.Serializable

@Serializable
internal data class UniOneErrorResponse(
    val status: String,
    val code: Int,
    val message: String
)