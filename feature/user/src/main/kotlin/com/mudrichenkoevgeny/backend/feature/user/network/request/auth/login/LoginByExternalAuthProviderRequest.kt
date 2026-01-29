package io.github.mudrichenkoevgeny.backend.feature.user.network.request.auth.login

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginByExternalAuthProviderRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.AUTH_PROVIDER)
    val authProvider: String,

    @NotBlankStringField
    @SerialName(UserApiFields.TOKEN)
    val token: String
)