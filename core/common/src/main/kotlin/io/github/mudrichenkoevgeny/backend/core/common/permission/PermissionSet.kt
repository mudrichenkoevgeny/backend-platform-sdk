package io.github.mudrichenkoevgeny.backend.core.common.permission

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode

/**
 * Generic pair of permission codes for read access with two sensitivity levels.
 *
 * Useful in managers/use cases that support:
 * - returning data with masked sensitive fields,
 * - returning data unmasked,
 * - denying access when neither permission is granted.
 *
 * The concrete domain (audit events, user identifiers, etc.) is defined by the caller.
 *
 * @param masked caller may read data only in masked/redacted form.
 * @param unmasked caller may read data in full (unmasked) form.
 */
data class PermissionSet(
    val masked: PermissionCode,
    val unmasked: PermissionCode
)