package com.mudrichenkoevgeny.backend.feature.user.network.response.auth

import com.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import com.mudrichenkoevgeny.backend.feature.user.network.response.token.SessionTokenResponse
import com.mudrichenkoevgeny.backend.feature.user.network.response.user.UserResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthDataResponse(
    @SerialName(UserApiFields.USER)
    val userResponse: UserResponse,

    @SerialName(UserApiFields.SESSION_TOKEN)
    val sessionTokenResponse: SessionTokenResponse
)