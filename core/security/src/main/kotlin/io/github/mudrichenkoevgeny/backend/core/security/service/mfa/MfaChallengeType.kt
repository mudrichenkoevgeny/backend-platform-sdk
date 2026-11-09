package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import kotlinx.serialization.Serializable

/**
 * Defines the specific security context for an MFA challenge.
 *
 * Ensures that a challenge token (mfaToken) issued for one purpose (e.g., login)
 * cannot be reused for another (e.g., disabling security settings).
 */
@Serializable
@JvmInline
public value class MfaChallengeType(
    public val value: String
) {
    public companion object {
        /**
         * Standard second-factor verification during the login flow.
         *
         * **Session Requirement:** None (sessionId is null).
         */
        public val LOGIN_TOTP: MfaChallengeType = MfaChallengeType("login_totp")

        /**
         * Backup-factor verification using a static recovery code when the
         * TOTP device is unavailable.
         *
         * **Session Requirement:** None (sessionId is null).
         */
        public val LOGIN_RECOVERY_CODE: MfaChallengeType = MfaChallengeType("login_recovery_code")

        /**
         * Initial setup and activation of the TOTP factor.
         *
         * **Session Requirement:** Mandatory (must match the current authenticated session).
         */
        public val SETUP_TOTP: MfaChallengeType = MfaChallengeType("setup_totp")

        /**
         * Re-authentication of an existing session to perform sensitive operations.
         *
         * **Session Requirement:** Mandatory (must match the current authenticated session).
         */
        public val STEP_UP: MfaChallengeType = MfaChallengeType("step_up")
    }
}
