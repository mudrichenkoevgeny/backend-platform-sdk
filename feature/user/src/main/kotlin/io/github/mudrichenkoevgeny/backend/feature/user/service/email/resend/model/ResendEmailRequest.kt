package io.github.mudrichenkoevgeny.backend.feature.user.service.email.resend.model

import kotlinx.serialization.Serializable

/**
 * Resend API request payload for sending an email.
 */
@Serializable
data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String
)