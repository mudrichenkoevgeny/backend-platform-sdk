package io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.EmailService
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser

/**
 * Stable email template identifiers used by [EmailService] implementations.
 *
 * Keys are expected to exist in localization resources loaded by [EmailParser].
 */
public object EmailTemplateKeys {
    /** Email ownership verification code template. */
    public const val VERIFICATION_CODE: String = "verification_code"

    /** Reset password verification code template. */
    public const val RESET_PASSWORD_CODE: String = "reset_password_code"

    /** Account unlock verification code template. */
    public const val UNLOCK_ACCOUNT_CODE: String = "unlock_account_code"

    /** Security notification: email already registered. */
    public const val ALREADY_REGISTERED: String = "already_registered"

    /** Welcome email after registration. */
    public const val SUCCESSFUL_REGISTRATION: String = "successful_registration"

    /** Security notification: successful login. */
    public const val SUCCESSFUL_LOGIN: String = "successful_login"

    /** Security notification: login from new device. */
    public const val NEW_DEVICE_LOGIN: String = "new_device_login"

    /** Security notification: password changed. */
    public const val PASSWORD_CHANGED: String = "password_changed"
}
