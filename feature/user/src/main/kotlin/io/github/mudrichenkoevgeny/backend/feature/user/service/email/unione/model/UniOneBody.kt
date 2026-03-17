package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import kotlinx.serialization.Serializable

/**
 * UniOne message body payload.
 *
 * @property html HTML content to send.
 */
@Serializable
data class UniOneBody(val html: String)