package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model

/**
 * Defines security requirement levels for OpenAPI route documentation.
 */
public enum class SecurityRequirementType {
    NONE,
    SENSITIVE_STEP_UP,
    SENSITIVE_STEP_UP_TOTP_REQUIRED,
    SENSITIVE_LOGIN_CHALLENGE
}
