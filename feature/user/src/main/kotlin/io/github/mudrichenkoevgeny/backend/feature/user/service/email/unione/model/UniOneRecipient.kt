package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import kotlinx.serialization.Serializable

/**
 * UniOne recipient payload.
 */
@Serializable
data class UniOneRecipient(val email: String)