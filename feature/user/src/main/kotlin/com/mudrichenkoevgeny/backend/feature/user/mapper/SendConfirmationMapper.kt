package com.mudrichenkoevgeny.backend.feature.user.mapper

import com.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import com.mudrichenkoevgeny.backend.feature.user.network.response.confirmation.SendConfirmationResponse

fun SendConfirmation.toResponse(): SendConfirmationResponse = SendConfirmationResponse(
    retryAfterSeconds = retryAfterSeconds
)