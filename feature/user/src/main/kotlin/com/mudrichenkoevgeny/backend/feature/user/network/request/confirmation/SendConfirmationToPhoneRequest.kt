package io.github.mudrichenkoevgeny.backend.feature.user.network.request.confirmation

import io.github.mudrichenkoevgeny.backend.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendConfirmationToPhoneRequest(
    @NotBlankStringField
    @SerialName(UserApiFields.PHONE_NUMBER)
    val phoneNumber: String
)