package io.github.mudrichenkoevgeny.backend.feature.user.service.phone

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

/**
 * Phone delivery service for user-related notifications.
 *
 * Implementations are expected to send SMS or similar messages containing verification codes
 * and security notifications.
 */
interface PhoneService {
    /** Sends a verification code to confirm phone ownership. */
    fun sendVerificationCode(phoneNumber: String, code: String): AppResult<Unit>
    /** Notifies that the phone number is already registered (security notification). */
    fun sendAlreadyRegisteredPhoneNumber(phoneNumber: String, ipAddress: String?, deviceName: String?): AppResult<Unit>
}