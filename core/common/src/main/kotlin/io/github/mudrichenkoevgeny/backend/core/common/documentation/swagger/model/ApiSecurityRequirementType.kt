package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model

enum class SecurityRequirementType {
    NONE,
    /** MFA Step-up, Reauthentication check */
    SENSITIVE_STEP_UP,
    /** Initial MFA challenge during login flow */
    SENSITIVE_LOGIN_CHALLENGE
}