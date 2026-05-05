package io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter

import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.model.SecurityRequirementType

/**
 * Formats a Markdown string for Swagger documentation by appending structured Authorization
 * and Security sections.
 *
 * This formatter ensures compliance with the project's API Documentation Standard,
 * including role-based access control, account status requirements, and MFA sensitivity notes.
 *
 * @param description The base functional description of the endpoint.
 * @param allowedRoles Set of user roles. If empty, defaults to "None".
 * @param allowedAccountStatuses Set of user account statuses. If empty, defaults to "None".
 * @param requiredPermissions Set of permission codes. Uses (**AND** semantics).
 * Defaults to [emptySet] ("None").
 * @param isPublic Whether the endpoint is accessible without authentication.
 * Influences the "Public Access" label.
 * @param hasAuthorityRequirement If `true`, appends standard text regarding
 * hierarchical Authority Level checks.
 * @param securityType Defines the [SecurityRequirementType] to append specific
 * MFA or session verification notes.
 *
 * @return A formatted Markdown string containing the description followed by
 * standardized Auth and Security blocks.
 */
fun getFormattedDescription(
    description: String,
    allowedRoles: Set<String>,
    allowedAccountStatuses: Set<String>,
    requiredPermissions: Set<String> = emptySet(),
    isPublic: Boolean = false,
    hasAuthorityRequirement: Boolean = false,
    securityType: SecurityRequirementType = SecurityRequirementType.NONE
): String {
    val publicAccess = if (isPublic) {
        SwaggerDocConstants.ACCESS_ALLOWED
    } else {
        SwaggerDocConstants.ACCESS_DENIED
    }

    val rolesText = if (allowedRoles.isEmpty()) {
        SwaggerDocConstants.NONE
    } else {
        "${allowedRoles.joinToString(", ")} ${SwaggerDocConstants.OR_SEMANTICS}"
    }

    val statusesText = if (allowedAccountStatuses.isEmpty()) {
        SwaggerDocConstants.NONE
    } else {
        "${allowedAccountStatuses.joinToString(", ")} ${SwaggerDocConstants.OR_SEMANTICS}"
    }

    val permissionsText = if (requiredPermissions.isEmpty()) {
        SwaggerDocConstants.NONE
    } else {
        "${requiredPermissions.joinToString(", ")} ${SwaggerDocConstants.AND_SEMANTICS}"
    }

    val authSection = StringBuilder().apply {
        append(SwaggerDocConstants.AUTH_SECTION_HEADER)
        append("\n${SwaggerDocConstants.PUBLIC_ACCESS_LABEL} $publicAccess")
        append("\n${SwaggerDocConstants.ALLOWED_ROLES_LABEL} $rolesText")
        append("\n${SwaggerDocConstants.ALLOWED_STATUSES_LABEL} $statusesText")
        append("\n${SwaggerDocConstants.REQUIRED_PERMISSIONS_LABEL} $permissionsText")

        if (hasAuthorityRequirement) {
            append("\n${SwaggerDocConstants.AUTHORITY_LEVEL_LABEL} ${SwaggerDocConstants.AUTHORITY_REQUIREMENT_TEXT}")
        }
    }.toString()

    val securitySection = when (securityType) {
        SecurityRequirementType.SENSITIVE_STEP_UP -> "\n\n${SwaggerDocConstants.SECURITY_STEP_UP_TEXT}"
        SecurityRequirementType.SENSITIVE_LOGIN_CHALLENGE -> "\n\n${SwaggerDocConstants.SECURITY_LOGIN_CHALLENGE_TEXT}"
        SecurityRequirementType.NONE -> ""
    }

    return description + authSection + securitySection
}