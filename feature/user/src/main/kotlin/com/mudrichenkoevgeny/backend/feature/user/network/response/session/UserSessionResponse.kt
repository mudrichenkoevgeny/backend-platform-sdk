package io.github.mudrichenkoevgeny.backend.feature.user.network.response.session

import io.github.mudrichenkoevgeny.backend.core.common.network.constants.ApiFields
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSessionResponse(
    @SerialName(ApiFields.ID)
    val id: String,

    @SerialName(UserApiFields.IDENTIFIER_ID)
    val identifierId: String,

    @SerialName(UserApiFields.IDENTIFIER_AUTH_PROVIDER)
    val identifierAuthProvider: String,

    @SerialName(UserApiFields.EXPIRES_AT)
    val expiresAt: Long? = null,

    @SerialName(UserApiFields.USER_AGENT)
    val userAgent: String? = null,

    @SerialName(UserApiFields.IP_ADDRESS)
    val ipAddress: String? = null,

    @SerialName(UserApiFields.DEVICE_NAME)
    val deviceName: String? = null,

    @SerialName(ApiFields.CREATED_AT)
    val createdAt: Long? = null,

    @SerialName(UserApiFields.LAST_ACCESSED_AT)
    val lastAccessedAt: Long? = null
)