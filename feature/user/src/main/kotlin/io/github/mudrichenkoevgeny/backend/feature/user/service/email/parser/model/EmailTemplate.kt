package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

import kotlinx.serialization.Serializable

@Serializable
data class EmailTemplate(
    val subject: String,
    val body: String
)