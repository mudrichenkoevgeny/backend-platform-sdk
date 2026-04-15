package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.ListingParamNames
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import kotlin.time.Instant

/**
 * Data access for users (persistence and querying).
 *
 * Implementations are responsible for storing user rows and returning paginated lists
 * with optional filters.
 */
interface UserRepository {
    suspend fun createUser(user: UserDetails): AppResult<UserDetails>

    suspend fun deleteUser(userId: UserId): AppResult<Unit>

    suspend fun updateUser(
        user: UserDetails,
        status: UserAccountStatus? = null,
        statusBeforeDeletion: UserAccountStatus? = null,
        permissions: Set<PermissionCode> = setOf(),
        lastLoginAt: Instant? = null,
        lastActiveAt: Instant? = null,
        scheduledPermanentDeletionAt: Instant? = null
    ): AppResult<UserDetails>

    suspend fun getUserById(userId: UserId): AppResult<UserDetails?>

    /**
     * Returns a page of [UserDetails] rows matching optional filters, ordered by [sortBy] and [sortOrder].
     *
     * **Pagination** (same semantics as [ListingParamNames.Pagination]): [PageParams.page] is the one-based
     * page index ([ListingParamNames.Pagination.PAGE_NUMBER]); [PageParams.size] is
     * [ListingParamNames.Pagination.PAGE_SIZE].
     *
     * **Sort** (same semantics as [ListingParamNames.Sort]): [sortBy] is the list `sort_by`
     * ([UserSortValues.UserSortBy] wire); [sortOrder] is `sort_order` ([SortOrder] wire).
     *
     * **Filters** align with the management user list API (user filter axis names / payload fields):
     * absent parameter means no filter on that axis. Non-null parameters combine as **AND**. Repeating the same
     * filter key as **OR** is not expressed here; compose that at a higher layer (e.g. HTTP handler) if needed.
     *
     * - [role] — [UserDetails.role].
     * - [accountStatus] — [UserDetails.accountStatus].
     * - [accountStatusBeforeDeletion] — [UserDetails.accountStatusBeforeDeletion].
     * - [userPermissionCode] — user has this code in [UserDetails.permissions].
     *
     * @param accessFilter Row-level role visibility applied before optional filters.
     * @param pageParams One-based page and page size.
     * @param sortBy Sort field for the listing.
     * @param sortOrder Sort direction.
     * @param role Filter by role.
     * @param accountStatus Filter by current account status.
     * @param accountStatusBeforeDeletion Filter by stored pre-deletion account status.
     * @param userPermissionCode Filter by presence of this permission in the user's permission set.
     * @return [PagedResult] of matching users or an error.
     */
    suspend fun getUsersList(
        accessFilter: UserRoleAccessFilter,
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy = UserSortValues.UserSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        role: UserRole? = null,
        accountStatus: UserAccountStatus? = null,
        accountStatusBeforeDeletion: UserAccountStatus? = null,
        userPermissionCode: PermissionCode? = null
    ): AppResult<PagedResult<UserDetails>>

    /**
     * Deletes users whose scheduled permanent deletion time has passed relative to [asOf].
     *
     * A row matches when [UserDetails.scheduledPermanentDeletionAt] is non-null and
     * `scheduledPermanentDeletionAt <= asOf` (same instant semantics as on the wire / domain model).
     *
     * Callers typically pass `Clock.System.now()` from a scheduled job; a fixed [asOf] keeps tests deterministic.
     *
     * @param asOf Upper bound instant for eligibility (inclusive).
     * @return Count of deleted rows, or an error when persistence fails.
     */
    suspend fun deleteUsersDueForPermanentDeletion(asOf: Instant): AppResult<Int>
}
