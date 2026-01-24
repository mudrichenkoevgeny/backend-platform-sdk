package com.mudrichenkoevgeny.backend.feature.user.network.request.auth.refreshtoken

import com.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import com.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.REFRESH_TOKEN)
    val refreshToken: String
)