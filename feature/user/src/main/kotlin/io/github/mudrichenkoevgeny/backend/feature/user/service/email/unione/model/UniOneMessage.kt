package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UniOneMessage(
    val recipients: List<UniOneRecipient>,
    val subject: String,
    @SerialName("from_email")
    val fromEmail: String,
    @SerialName("from_name")
    val fromName: String,
    val body: UniOneBody,
    @SerialName("track_domain")
    val trackDomain: String
)