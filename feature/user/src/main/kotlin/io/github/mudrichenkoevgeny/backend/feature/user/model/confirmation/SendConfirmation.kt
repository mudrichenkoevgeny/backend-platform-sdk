package io.github.mudrichenkoevgeny.backend.feature.user.model.confirmation

/**
 * Result of sending a confirmation code/message (email/SMS/etc.).
 *
 * @property retryAfterSeconds Throttle delay in seconds before the next attempt is allowed.
 */
data class SendConfirmation(
    val retryAfterSeconds: Int
)