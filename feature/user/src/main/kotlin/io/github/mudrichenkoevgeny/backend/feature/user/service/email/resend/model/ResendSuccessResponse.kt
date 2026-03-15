package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

import kotlinx.serialization.Serializable

@Serializable
data class ResendSuccessResponse(
    val id: String
)