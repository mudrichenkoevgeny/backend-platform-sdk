package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * UniOne API request payload for sending an email.
 */
@Serializable
data class UniOneEmailRequest(
    @SerialName("api_key") val apiKey: String,
    val message: UniOneMessage
)