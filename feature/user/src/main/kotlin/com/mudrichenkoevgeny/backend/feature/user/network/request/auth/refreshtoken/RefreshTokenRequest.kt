package io.github.mudrichenkoevgeny.backend.feature.user.network.request.auth.refreshtoken

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.REFRESH_TOKEN)
    val refreshToken: String
)