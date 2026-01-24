package com.mudrichenkoevgeny.backend.feature.user.network.response.token

import com.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionTokenResponse(
    @SerialName(UserApiFields.ACCESS_TOKEN)
    val accessToken: String,

    @SerialName(UserApiFields.REFRESH_TOKEN)
    val refreshToken: String,

    @SerialName(UserApiFields.EXPIRES_AT)
    val expiresAt: Long,

    @SerialName(UserApiFields.TOKEN_TYPE)
    val tokenType: String
)