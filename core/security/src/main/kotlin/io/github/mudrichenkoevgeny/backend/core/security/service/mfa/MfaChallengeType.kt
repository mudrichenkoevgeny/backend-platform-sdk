package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Defines the specific security context for an MFA challenge.
 *
 * Ensures that a challenge token (mfaToken) issued for one purpose (e.g., login)
 * cannot be reused for another (e.g., disabling security settings).
 */
@Serializable
@JvmInline
value class MfaChallengeType(val value: String) {
    companion object {
        /**
         * Standard second-factor verification during the login flow.
         *
         * **Session Requirement:** None (sessionId is null).
         */
        val LOGIN_TOTP = MfaChallengeType("login_totp")

        /**
         * Backup-factor verification using a static recovery code when the
         * TOTP device is unavailable.
         *
         * **Session Requirement:** None (sessionId is null).
         */
        val LOGIN_RECOVERY_CODE = MfaChallengeType("login_recovery_code")

        /**
         * Initial setup and activation of the TOTP factor.
         *
         * **Session Requirement:** Mandatory (must match the current authenticated session).
         */
        val SETUP_TOTP = MfaChallengeType("setup_totp")

        /**
         * Re-authentication of an existing session to perform sensitive operations.
         *
         * **Session Requirement:** Mandatory (must match the current authenticated session).
         */
        val STEP_UP = MfaChallengeType("step_up")
    }
}