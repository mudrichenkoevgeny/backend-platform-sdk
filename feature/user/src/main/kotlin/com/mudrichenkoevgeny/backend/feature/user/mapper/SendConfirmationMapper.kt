package io.github.mudrichenkoevgeny.backend.feature.user.mapper

import io.github.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import io.github.mudrichenkoevgeny.backend.feature.user.network.response.confirmation.SendConfirmationResponse

fun SendConfirmation.toResponse(): SendConfirmationResponse = SendConfirmationResponse(
    retryAfterSeconds = retryAfterSeconds
)