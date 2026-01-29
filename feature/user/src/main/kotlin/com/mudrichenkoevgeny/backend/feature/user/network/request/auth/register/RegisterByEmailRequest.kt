package io.github.mudrichenkoevgeny.backend.feature.user.network.request.auth.register

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterByEmailRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.EMAIL)
    val email: String,

    @NotBlankStringField
    @SerialName(UserApiFields.PASSWORD)
    val password: String,

    @NotBlankStringField
    @SerialName(UserApiFields.CONFIRMATION_CODE)
    val confirmationCode: String
)