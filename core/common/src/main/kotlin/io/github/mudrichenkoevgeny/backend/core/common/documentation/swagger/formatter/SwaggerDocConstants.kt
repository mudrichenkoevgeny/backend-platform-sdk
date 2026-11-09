package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter

public object SwaggerDocConstants {
    public const val AUTH_SECTION_HEADER: String = "\n\n---\n**Authorization**"
    public const val PUBLIC_ACCESS_LABEL: String = "- **Public Access:**"
    public const val ALLOWED_ROLES_LABEL: String = "- **Allowed Roles:**"
    public const val ALLOWED_STATUSES_LABEL: String = "- **Allowed Account Statuses:**"
    public const val REQUIRED_PERMISSIONS_LABEL: String = "- **Required Permissions:**"
    public const val AUTHORITY_LEVEL_LABEL: String = "- **Authority Level:**"

    public const val OR_SEMANTICS: String = "(**OR** semantics)"
    public const val AND_SEMANTICS: String = "(**AND** semantics)"
    public const val ACCESS_ALLOWED: String = "Allowed"
    public const val ACCESS_DENIED: String = "Denied"
    public const val ANY: String = "Any"
    public const val NONE: String = "None"

    public const val AUTHORITY_REQUIREMENT_TEXT: String =
        "Actor's level must be strictly greater than the target's level. " +
            "Actor cannot target own account."

    public const val SECURITY_STEP_UP_TEXT: String =
        "**Security:** Sensitive operation. MFA Step-up required (if enabled for the account). " +
            "Returns [SecurityErrorCodes.MFA_CONFIRMATION_REQUIRED] if additional " +
            "verification is needed. Session must be verified via re-authentication route " +
            "([OpenSessionRoutes.REAUTHENTICATE_SESSION] / " +
            "[SelfManagementSessionRoutes.REAUTHENTICATE_SESSION]) if stale."

    public const val SECURITY_TOTP_REQUIRED_TEXT: String =
        "**Security:** Sensitive operation. Requires active TOTP on the account; " +
            "returns [SecurityErrorCodes.TOTP_NOT_ENABLED] if TOTP is not configured. " +
            "When enabled, MFA Step-up is required via [SecurityErrorCodes.MFA_CONFIRMATION_REQUIRED]. " +
            "Session must be verified via re-authentication route " +
            "([OpenSessionRoutes.REAUTHENTICATE_SESSION] / " +
            "[SelfManagementSessionRoutes.REAUTHENTICATE_SESSION]) if stale."

    public const val SECURITY_LOGIN_CHALLENGE_TEXT: String =
        "**Security:** Sensitive operation. If MFA is enabled for the account, " +
            "this method returns a [SecurityErrorCodes.MFA_CONFIRMATION_REQUIRED] " +
            "error and a challenge token. " +
            "The process must be completed via [LOGIN_BY_TOTP] or [LOGIN_BY_TOTP_RECOVERY_CODE]."
}
