package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model

enum class SecurityRequirementType {
    NONE,

    /** MFA Step-up, Reauthentication check (if MFA/TOTP is enabled on the account) */
    SENSITIVE_STEP_UP,

    /** Sensitive operation that strictly requires active TOTP on the account */
    SENSITIVE_STEP_UP_TOTP_REQUIRED,

    /** Initial MFA challenge during login flow */
    SENSITIVE_LOGIN_CHALLENGE
}