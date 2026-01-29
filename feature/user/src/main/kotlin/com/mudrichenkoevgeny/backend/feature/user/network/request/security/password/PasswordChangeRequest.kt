package io.github.mudrichenkoevgeny.backend.feature.user.network.request.security.password

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PasswordChangeRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.EMAIL)
    val email: String,

    @NotBlankStringField
    @SerialName(UserApiFields.OLD_PASSWORD)
    val oldPassword: String,

    @NotBlankStringField
    @SerialName(UserApiFields.NEW_PASSWORD)
    val newPassword: String
)