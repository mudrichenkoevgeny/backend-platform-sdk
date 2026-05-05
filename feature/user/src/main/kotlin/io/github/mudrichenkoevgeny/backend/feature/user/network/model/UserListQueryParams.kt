package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole

/**
 * Data class representing filtered search parameters for fetching user lists in management contexts.
 *
 * Combines standard [ListingQueryParams] (pagination and sorting) with domain-specific filters
 * such as roles, account statuses, and authority levels.
 *
 * @property listing Pagination, [UserSortValues.UserSortBy] field, and sort direction.
 * @property roles Filter by one or more [UserRole] (e.g., ADMIN, STAFF).
 * @property accountStatuses Filter by current [UserAccountStatus] (e.g., ACTIVE, BANNED).
 * @property accountStatusesBeforeDeletion Filter by the status a user had before moving to PENDING_DELETION.
 * @property authorityLevelFrom Lower bound for authority level filtering (inclusive).
 * @property authorityLevelTo Upper bound for authority level filtering (inclusive).
 * @property requiredPermissionCodes Filter for users possessing all specified [PermissionCode]s.
 * @property isTotpEnabled Filter users based on whether Two-Factor Authentication is currently enabled.
 */
data class UserListQueryParams(
    val listing: ListingQueryParams<UserSortValues.UserSortBy>,
    val roles: List<UserRole>,
    val accountStatuses: List<UserAccountStatus>,
    val accountStatusesBeforeDeletion: List<UserAccountStatus>,
    val authorityLevelFrom: Int?,
    val authorityLevelTo: Int?,
    val requiredPermissionCodes: Set<PermissionCode>,
    val isTotpEnabled: Boolean?
)