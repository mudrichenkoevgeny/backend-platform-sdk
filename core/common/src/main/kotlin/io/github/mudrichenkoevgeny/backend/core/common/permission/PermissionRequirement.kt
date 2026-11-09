package io.github.mudrichenkoevgeny.backend.core.common.permission

/**
 * Permission requirement level for accessing resource fields.
 */
public enum class PermissionRequirement {
    UNMASKED,
    MASKED,
    FORBIDDEN
}
