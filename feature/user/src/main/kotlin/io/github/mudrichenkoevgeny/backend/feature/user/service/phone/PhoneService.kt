package io.github.mudrichenkoevgeny.backend.feature.user.service.phone

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

/**
 * Phone delivery service for user-related notifications.
 *
 * Implementations are expected to send SMS or similar messages containing verification codes
 * and security notifications.
 */
public interface PhoneService {
    /** Sends a verification code to confirm phone ownership. */
    public suspend fun sendVerificationCode(
        phoneNumber: String,
        code: String,
        language: String?
    ): AppResult<Unit>

    /** Sends a verification code for unlock account flow. */
    public suspend fun sendUnlockAccountVerificationCode(
        phoneNumber: String,
        code: String,
        language: String?
    ): AppResult<Unit>

    /** Notifies that the phone number is already registered (security notification). */
    public suspend fun sendAlreadyRegisteredPhoneNumber(
        phoneNumber: String,
        ipAddress: String?,
        deviceName: String?,
        language: String?
    ): AppResult<Unit>

    /** Simulate a successful SMS send. */
    public suspend fun fakeSendSMS(): AppResult<Unit>
}
