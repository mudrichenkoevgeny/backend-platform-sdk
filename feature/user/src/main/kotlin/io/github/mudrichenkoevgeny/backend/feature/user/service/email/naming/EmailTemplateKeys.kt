package io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming

/**
 * Stable email template identifiers used by [EmailService] implementations.
 *
 * Keys are expected to exist in localization resources loaded by [EmailParser].
 */
object EmailTemplateKeys {
    /** Email ownership verification code template. */
    const val VERIFICATION_CODE = "verification_code"
    /** Reset password verification code template. */
    const val RESET_PASSWORD_CODE = "reset_password_code"
    /** Security notification: email already registered. */
    const val ALREADY_REGISTERED = "already_registered"
    /** Welcome email after registration. */
    const val SUCCESSFUL_REGISTRATION = "successful_registration"
    /** Security notification: successful login. */
    const val SUCCESSFUL_LOGIN = "successful_login"
    /** Security notification: password changed. */
    const val PASSWORD_CHANGED = "password_changed"
}