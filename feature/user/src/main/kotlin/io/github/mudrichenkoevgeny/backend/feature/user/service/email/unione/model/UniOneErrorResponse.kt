package io.github.mudrichenkoevgeny.backend.feature.user.service.email.unione.model

import kotlinx.serialization.Serializable

/**
 * UniOne API error payload returned for non-2xx responses.
 */
@Serializable
internal data class UniOneErrorResponse(
    val status: String,
    val code: Int,
    val message: String
)