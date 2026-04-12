package io.github.mudrichenkoevgeny.backend.feature.audit.api.domain.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode

/**
 * Pair of permission codes that describe how strongly a caller may **read** a single audit event
 * for a given actor category (system, service, user role, etc.).
 *
 * Used by `AuditManagerImpl` when deciding whether the loaded row may be returned **unmasked**, **masked**
 * (sensitive fields redacted via `AuditDataMasker` in `core/audit`), or omitted from the result.
 *
 * @param masked Caller may see the event but sensitive fields must be redacted.
 * @param unmasked Caller may see the event with original values where policy allows.
 *
 * Both are distinct foundation `PermissionCode` values (typically from `AuditPermissionCode` in shared foundation).
 */
data class AuditActorPermissionSet(
    val masked: PermissionCode,
    val unmasked: PermissionCode
)
