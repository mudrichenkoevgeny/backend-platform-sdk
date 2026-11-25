package io.github.mudrichenkoevgeny.backend.feature.user.domain.model

import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.user.UserRole

/**
 * Generic row-level visibility predicate based on owner user roles.
 *
 * Repository applies this as:
 * - owner role is in [allowedUserRoles]
 *
 * If [allowedUserRoles] is empty, listing returns no rows.
 */
public data class UserRoleAccessFilter(
    val allowedUserRoles: Set<UserRole>
)
