package io.github.mudrichenkoevgeny.backend.feature.user.mapper.confirmation

import io.github.mudrichenkoevgeny.backend.feature.user.model.confirmation.SendConfirmation
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.confirmation.SendConfirmationResponse

/**
 * Maps internal [SendConfirmation] to the shared network response contract.
 */
fun SendConfirmation.toSendConfirmationResponse(): SendConfirmationResponse = SendConfirmationResponse(
    retryAfterSeconds = retryAfterSeconds
)