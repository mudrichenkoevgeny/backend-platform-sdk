package io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user

import io.github.mudrichenkoevgeny.backend.core.common.model.UpdateField
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.UserRoleAccessFilter
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
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

    /**
     * Persists a new user record.
     *
     * @param user user details model to create.
     * @return [AppResult] containing the created [UserDetails] or an error.
     */
    suspend fun createUser(user: UserDetails): AppResult<UserDetails>

    /**
     * Deletes a user record by the unique user identifier.
     *
     * @param userId unique user identifier.
     * @return [AppResult] containing [Unit] on success or an error.
     */
    suspend fun deleteUser(userId: UserId): AppResult<Unit>

    /**
     * Updates user account fields.
     *
     * Each parameter uses [UpdateField] to distinguish between:
     * - [UpdateField.Ignore]: Field will not be included in the update.
     * - [UpdateField.Set]: Field will be updated to the provided value (including null).
     *
     * @param userId unique identifier of the user to update.
     * @param status new account status.
     * @param statusBeforeDeletion status to preserve during the deletion process.
     * @param authorityLevel new authority level.
     * @param permissionCodes new set of effective permissions.
     * @param isTotpEnabled TOTP enablement status.
     * @param lastLoginAt timestamp of the last successful authentication.
     * @param lastActiveAt timestamp of the last user activity.
     * @param scheduledPermanentDeletionAt timestamp for the final removal of the account.
     * @param accountLockoutType account lockout type.
     * @param temporaryLockoutUntil timestamp when temporary lockout expires.
     * @return [AppResult] containing the updated [UserDetails].
     */
    suspend fun updateUser(
        userId: UserId,
        status: UpdateField<UserAccountStatus> = UpdateField.Ignore,
        statusBeforeDeletion: UpdateField<UserAccountStatus> = UpdateField.Ignore,
        authorityLevel: UpdateField<Int> = UpdateField.Ignore,
        permissionCodes: UpdateField<Set<PermissionCode>> = UpdateField.Ignore,
        isTotpEnabled: UpdateField<Boolean> = UpdateField.Ignore,
        lastLoginAt: UpdateField<Instant> = UpdateField.Ignore,
        lastActiveAt: UpdateField<Instant> = UpdateField.Ignore,
        scheduledPermanentDeletionAt: UpdateField<Instant> = UpdateField.Ignore,
        accountLockoutType: UpdateField<AccountLockoutType> = UpdateField.Ignore,
        temporaryLockoutUntil: UpdateField<Instant> = UpdateField.Ignore
    ): AppResult<UserDetails>

    /**
     * Retrieves user details by the unique user identifier.
     *
     * @param userId unique user identifier.
     * @return [AppResult] containing [UserDetails] when found, `null` when missing, or an error.
     */
    suspend fun getUserDetailsById(userId: UserId): AppResult<UserDetails?>

    /**
     * Returns a page of [UserDetails] rows matching optional filters, ordered by [sortBy] and [sortOrder].
     *
     * **Filters** align with the management user list API:
     * Empty list means no filter on that axis. Non-empty categories combine as **AND**,
     * while values within a single list category combine as **OR** (SQL IN).
     *
     * @param accessFilter Row-level role visibility applied before optional filters.
     * @param pageParams One-based page and page size.
     * @param sortBy Sort field for the listing.
     * @param sortOrder Sort direction.
     * @param roles Filter by one or more roles.
     * @param accountStatuses Filter by one or more account statuses.
     * @param accountStatusesBeforeDeletion Filter by one or more pre-deletion account statuses.
     * @param authorityLevelFrom optional inclusive lower bound for authority level.
     * @param authorityLevelTo optional inclusive upper bound for authority level.
     * @param permissionCodes list of permission codes that the user MUST possess (ALL of them).
     * @param isTotpEnabled optional filter by TOTP enablement status.
     * // todo wait for shared update and add lockout fields
     * @return [PagedResult] of matching users or an error.
     */
    suspend fun getUsersPageWithAccessFilter(
        accessFilter: UserRoleAccessFilter = UserRoleAccessFilter(emptySet()),
        pageParams: PageParams,
        sortBy: UserSortValues.UserSortBy = UserSortValues.UserSortBy.CREATED_AT,
        sortOrder: SortOrder = SortOrder.DESC,
        roles: List<UserRole> = emptyList(),
        accountStatuses: List<UserAccountStatus> = emptyList(),
        accountStatusesBeforeDeletion: List<UserAccountStatus> = emptyList(),
        authorityLevelFrom: Int? = null,
        authorityLevelTo: Int? = null,
        permissionCodes: Set<PermissionCode> = emptySet(),
        isTotpEnabled: Boolean? = null
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

    /**
     * Finds users with an active temporary lockout whose [UserDetails.temporaryLockoutUntil] has passed relative to [asOf].
     *
     * @param asOf Upper bound instant for lockout expiration (inclusive).
     * @return List of matching [UserId]s, or an error.
     */
    suspend fun getExpiredTemporaryLockouts(asOf: Instant): AppResult<List<UserId>>
}