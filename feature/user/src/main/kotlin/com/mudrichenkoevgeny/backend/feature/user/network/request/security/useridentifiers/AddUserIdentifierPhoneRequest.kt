package io.github.mudrichenkoevgeny.backend.feature.user.network.request.security.useridentifiers

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddUserIdentifierPhoneRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.PHONE_NUMBER)
    val phoneNumber: String,

    @NotBlankStringField
    @SerialName(UserApiFields.CONFIRMATION_CODE)
    val confirmationCode: String
)