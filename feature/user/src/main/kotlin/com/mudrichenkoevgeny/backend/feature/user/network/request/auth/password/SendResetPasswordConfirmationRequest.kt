package io.github.mudrichenkoevgeny.backend.feature.user.network.request.auth.password

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendResetPasswordConfirmationRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.EMAIL)
    val email: String
)