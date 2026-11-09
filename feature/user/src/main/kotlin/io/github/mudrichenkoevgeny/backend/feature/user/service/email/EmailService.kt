package io.github.mudrichenkoevgeny.backend.feature.user.service.email

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

/**
 * Email delivery service for user-related notifications.
 *
 * Implementations may deliver emails via a third-party provider or may be a no-op/fallback implementation.
 * Most methods are templated and are expected to be localized using the provided language/locale hints.
 */
public interface EmailService {
    /** Sends a verification code to confirm email ownership. */
    public suspend fun sendVerificationCode(
        email: String,
        code: String,
        language: String?
    ): AppResult<Unit>

    /** Sends a verification code for password reset flow. */
    public suspend fun sendResetPasswordVerificationCode(
        email: String,
        code: String,
        language: String?
    ): AppResult<Unit>

    /** Sends a verification code for unlock account flow. */
    public suspend fun sendUnlockAccountVerificationCode(
        email: String,
        code: String,
        language: String?
    ): AppResult<Unit>

    /** Notifies that the email is already registered (security notification). */
    public suspend fun sendAlreadyRegisteredEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?,
        language: String?
    ): AppResult<Unit>

    /** Confirms successful registration. */
    public suspend fun sendSuccessfulRegistrationEmail(email: String): AppResult<Unit>

    /** Notifies about successful login (security notification). */
    public suspend fun sendSuccessfulLoginEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit>

    /** Notifies about a login from a new unrecognized device (security notification). */
    public suspend fun sendNewDeviceLoginEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?,
        userAgent: String?
    ): AppResult<Unit>

    /** Notifies that the password was changed. */
    public suspend fun sendPasswordSuccessfullyChangedEmail(
        email: String,
        ipAddress: String?,
        deviceName: String?
    ): AppResult<Unit>

    /** Simulate a successful email send. */
    public suspend fun fakeSendEmail(): AppResult<Unit>
}
