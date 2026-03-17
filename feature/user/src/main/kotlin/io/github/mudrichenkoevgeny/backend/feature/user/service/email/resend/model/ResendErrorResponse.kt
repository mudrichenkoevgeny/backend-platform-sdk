package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

import kotlinx.serialization.Serializable

/**
 * Resend API error payload returned for non-2xx responses.
 */
@Serializable
data class ResendErrorResponse(
    val message: String? = null,
    val name: String? = null
)