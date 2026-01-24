package com.mudrichenkoevgeny.backend.feature.user.network.response.confirmation

import com.mudrichenkoevgeny.backend.feature.user.network.constants.UserApiFields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SendConfirmationResponse(
    @SerialName(UserApiFields.RETRY_AFTER_SECONDS)
    val retryAfterSeconds: Int
)