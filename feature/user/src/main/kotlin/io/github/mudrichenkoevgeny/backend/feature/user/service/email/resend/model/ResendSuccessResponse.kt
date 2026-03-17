package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

import kotlinx.serialization.Serializable

/**
 * Resend API success payload for email send.
 */
@Serializable
data class ResendSuccessResponse(
    val id: String
)