package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

import kotlinx.serialization.Serializable

@Serializable
data class ResendErrorResponse(
    val message: String? = null,
    val name: String? = null
)