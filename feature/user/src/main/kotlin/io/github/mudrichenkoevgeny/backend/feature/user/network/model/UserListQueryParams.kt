package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import kotlin.time.Instant

/**
 * Data class representing filtered search parameters for fetching user lists in management contexts.
 *
 * Combines standard [ListingQueryParams] (pagination and sorting) with domain-specific filters
 * such as roles, account statuses, authority levels, profile fields, timestamps, and search query.
 */
data class UserListQueryParams(
    val listing: ListingQueryParams<UserSortBy>,
    val userIds: List<String> = emptyList(),
    val roles: List<UserRole> = emptyList(),
    val excludeRoles: List<UserRole> = emptyList(),
    val accountStatuses: List<UserAccountStatus> = emptyList(),
    val excludeAccountStatuses: List<UserAccountStatus> = emptyList(),
    val accountStatusesOnRestore: List<UserAccountStatus> = emptyList(),
    val excludeAccountStatusesOnRestore: List<UserAccountStatus> = emptyList(),
    val isAccountStatusOnRestoreSet: Boolean? = null,
    val authorityLevelFrom: Int? = null,
    val authorityLevelTo: Int? = null,
    val requiredPermissionCodes: Set<PermissionCode> = emptySet(),
    val excludePermissionCodes: Set<PermissionCode> = emptySet(),
    val isTotpEnabled: Boolean? = null,
    val accountLockoutTypes: List<AccountLockoutType> = emptyList(),
    val excludeAccountLockoutTypes: List<AccountLockoutType> = emptyList(),
    val nickname: String? = null,
    val isNicknameSet: Boolean? = null,
    val displayName: String? = null,
    val isDisplayNameSet: Boolean? = null,
    val isAvatarSet: Boolean? = null,
    val createdAtFrom: Instant? = null,
    val createdAtTo: Instant? = null,
    val updatedAtFrom: Instant? = null,
    val updatedAtTo: Instant? = null,
    val isUpdatedAtSet: Boolean? = null,
    val lastLoginAtFrom: Instant? = null,
    val lastLoginAtTo: Instant? = null,
    val isLastLoginAtSet: Boolean? = null,
    val lastActiveAtFrom: Instant? = null,
    val lastActiveAtTo: Instant? = null,
    val isLastActiveAtSet: Boolean? = null,
    val scheduledPermanentDeletionAtFrom: Instant? = null,
    val scheduledPermanentDeletionAtTo: Instant? = null,
    val isScheduledPermanentDeletionAtSet: Boolean? = null,
    val temporaryLockoutUntilFrom: Instant? = null,
    val temporaryLockoutUntilTo: Instant? = null,
    val isTemporaryLockoutUntilSet: Boolean? = null,
    val storageQuotaBytesFrom: Long? = null,
    val storageQuotaBytesTo: Long? = null,
    val isStorageQuotaBytesSet: Boolean? = null,
    val storageMaxUploadSizeBytesFrom: Long? = null,
    val storageMaxUploadSizeBytesTo: Long? = null,
    val isStorageMaxUploadSizeBytesSet: Boolean? = null,
    val storageMaxPendingUploadsFrom: Int? = null,
    val storageMaxPendingUploadsTo: Int? = null,
    val isStorageMaxPendingUploadsSet: Boolean? = null,
    val isStorageConfigurationSet: Boolean? = null,
    val birthdateFrom: Instant? = null,
    val birthdateTo: Instant? = null,
    val isBirthdateSet: Boolean? = null,
    val searchQuery: String? = null
)
