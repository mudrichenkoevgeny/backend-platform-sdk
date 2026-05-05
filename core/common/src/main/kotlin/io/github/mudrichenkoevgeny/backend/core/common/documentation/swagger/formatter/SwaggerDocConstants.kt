package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter

object SwaggerDocConstants {
    const val AUTH_SECTION_HEADER = "\n\n---\n**Authorization**"
    const val PUBLIC_ACCESS_LABEL = "- **Public Access:**"
    const val ALLOWED_ROLES_LABEL = "- **Allowed Roles:**"
    const val ALLOWED_STATUSES_LABEL = "- **Allowed Account Statuses:**"
    const val REQUIRED_PERMISSIONS_LABEL = "- **Required Permissions:**"
    const val AUTHORITY_LEVEL_LABEL = "- **Authority Level:**"

    const val OR_SEMANTICS = "(**OR** semantics)"
    const val AND_SEMANTICS = "(**AND** semantics)"
    const val ACCESS_ALLOWED = "Allowed"
    const val ACCESS_DENIED = "Denied"
    const val ANY = "Any"
    const val NONE = "None"

    const val AUTHORITY_REQUIREMENT_TEXT =
        "Actor's level must be strictly greater than the target's level. Actor cannot target own account."

    const val SECURITY_STEP_UP_TEXT =
        "**Security:** Sensitive operation. MFA Step-up required (if enabled). Returns [SecurityErrorCodes.TOTP_CONFIRMATION_REQUIRED] if additional verification is needed. Session must be verified via [OpenSessionRoutes.REAUTHENTICATE_SESSION] if stale."

    const val SECURITY_LOGIN_CHALLENGE_TEXT =
        "**Security:** Sensitive operation. If MFA is enabled for the account, this method returns a [SecurityErrorCodes.TOTP_CONFIRMATION_REQUIRED] error and a challenge token. The process must be completed via [LOGIN_BY_TOTP] or [LOGIN_BY_TOTP_RECOVERY_CODE]."
}