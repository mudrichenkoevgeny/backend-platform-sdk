package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

import kotlinx.serialization.Serializable
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser

/**
 * Localized email content template.
 *
 * Both fields may contain placeholders in `{argKey}` format that are replaced by an [EmailParser].
 *
 * @property subject email subject template
 * @property body email HTML body template
 */
@Serializable
data class EmailTemplate(
    val subject: String,
    val body: String
)