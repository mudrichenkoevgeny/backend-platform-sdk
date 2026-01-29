package io.github.mudrichenkoevgeny.backend.feature.user.network.response.auth

import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import io.github.mudrichenkoevgeny.backend.feature.user.network.response.token.SessionTokenResponse
import io.github.mudrichenkoevgeny.backend.feature.user.network.response.user.UserResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthDataResponse(
    @SerialName(UserApiFields.USER)
    val userResponse: UserResponse,

    @SerialName(UserApiFields.SESSION_TOKEN)
    val sessionTokenResponse: SessionTokenResponse
)