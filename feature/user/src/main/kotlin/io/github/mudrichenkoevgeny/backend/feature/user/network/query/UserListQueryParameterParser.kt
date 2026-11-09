package io.github.mudrichenkoevgeny.backend.feature.user.network.query

import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.firstNonBlankQueryValue
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.getQueryValues
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseBooleanQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseIntQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseListingQueryParams
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseLongQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseTimestampQueryOrNull
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.parseValidatedQueryList
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.model.UserListQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserFilterValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.ktor.server.application.ApplicationCall
import kotlin.uuid.Uuid

/**
 * Parses user management list query parameters with strict validation.
 */
public fun ApplicationCall.parseUsersListQueryParams(): UserListQueryParams {
    val filterNames = UserFilterValues

    return UserListQueryParams(
        listing = parseListingQueryParams(
            defaultSortBy = UserSortBy.DEFAULT,
            parseSortByOrNull = UserSortBy::fromValueOrNull
        ),
        userIds = parseValidatedQueryList(filterNames.USER_ID) { Uuid.parseOrNull(it)?.toString() },
        roles = parseValidatedQueryList(
            filterNames.ROLE,
            UserRole::fromValueOrNull
        ),
        excludeRoles = parseValidatedQueryList(
            filterNames.EXCLUDE_ROLE,
            UserRole::fromValueOrNull
        ),
        accountStatuses = parseValidatedQueryList(
            filterNames.ACCOUNT_STATUS,
            UserAccountStatus::fromValueOrNull
        ),
        excludeAccountStatuses = parseValidatedQueryList(
            filterNames.EXCLUDE_ACCOUNT_STATUS,
            UserAccountStatus::fromValueOrNull
        ),
        accountStatusesOnRestore = parseValidatedQueryList(
            filterNames.ACCOUNT_STATUS_ON_RESTORE,
            UserAccountStatus::fromValueOrNull
        ),
        excludeAccountStatusesOnRestore = parseValidatedQueryList(
            filterNames.EXCLUDE_ACCOUNT_STATUS_ON_RESTORE,
            UserAccountStatus::fromValueOrNull
        ),
        isAccountStatusOnRestoreSet = parseBooleanQueryOrNull(filterNames.IS_ACCOUNT_STATUS_ON_RESTORE_SET),
        authorityLevelFrom = parseIntQueryOrNull(filterNames.AUTHORITY_LEVEL_FROM),
        authorityLevelTo = parseIntQueryOrNull(filterNames.AUTHORITY_LEVEL_TO),
        requiredPermissionCodes = getQueryValues(filterNames.PERMISSION_CODES).mapToSet { PermissionCode(it) },
        excludePermissionCodes = getQueryValues(filterNames.EXCLUDE_PERMISSION_CODES).mapToSet { PermissionCode(it) },
        isTotpEnabled = parseBooleanQueryOrNull(filterNames.IS_TOTP_ENABLED),
        accountLockoutTypes = parseValidatedQueryList(
            filterNames.ACCOUNT_LOCKOUT_TYPE,
            AccountLockoutType::fromValueOrNull
        ),
        excludeAccountLockoutTypes = parseValidatedQueryList(
            filterNames.EXCLUDE_ACCOUNT_LOCKOUT_TYPE,
            AccountLockoutType::fromValueOrNull
        ),
        nickname = firstNonBlankQueryValue(filterNames.NICKNAME),
        isNicknameSet = parseBooleanQueryOrNull(filterNames.IS_NICKNAME_SET),
        displayName = firstNonBlankQueryValue(filterNames.DISPLAY_NAME),
        isDisplayNameSet = parseBooleanQueryOrNull(filterNames.IS_DISPLAY_NAME_SET),
        isAvatarSet = parseBooleanQueryOrNull(filterNames.IS_AVATAR_SET),
        createdAtFrom = parseTimestampQueryOrNull(filterNames.CREATED_AT_FROM),
        createdAtTo = parseTimestampQueryOrNull(filterNames.CREATED_AT_TO),
        updatedAtFrom = parseTimestampQueryOrNull(filterNames.UPDATED_AT_FROM),
        updatedAtTo = parseTimestampQueryOrNull(filterNames.UPDATED_AT_TO),
        isUpdatedAtSet = parseBooleanQueryOrNull(filterNames.IS_UPDATED_AT_SET),
        lastLoginAtFrom = parseTimestampQueryOrNull(filterNames.LAST_LOGIN_AT_FROM),
        lastLoginAtTo = parseTimestampQueryOrNull(filterNames.LAST_LOGIN_AT_TO),
        isLastLoginAtSet = parseBooleanQueryOrNull(filterNames.IS_LAST_LOGIN_AT_SET),
        lastActiveAtFrom = parseTimestampQueryOrNull(filterNames.LAST_ACTIVE_AT_FROM),
        lastActiveAtTo = parseTimestampQueryOrNull(filterNames.LAST_ACTIVE_AT_TO),
        isLastActiveAtSet = parseBooleanQueryOrNull(filterNames.IS_LAST_ACTIVE_AT_SET),
        scheduledPermanentDeletionAtFrom = parseTimestampQueryOrNull(filterNames.SCHEDULED_PERMANENT_DELETION_AT_FROM),
        scheduledPermanentDeletionAtTo = parseTimestampQueryOrNull(filterNames.SCHEDULED_PERMANENT_DELETION_AT_TO),
        isScheduledPermanentDeletionAtSet = parseBooleanQueryOrNull(filterNames.IS_SCHEDULED_PERMANENT_DELETION_AT_SET),
        temporaryLockoutUntilFrom = parseTimestampQueryOrNull(filterNames.TEMPORARY_LOCKOUT_UNTIL_FROM),
        temporaryLockoutUntilTo = parseTimestampQueryOrNull(filterNames.TEMPORARY_LOCKOUT_UNTIL_TO),
        isTemporaryLockoutUntilSet = parseBooleanQueryOrNull(filterNames.IS_TEMPORARY_LOCKOUT_UNTIL_SET),
        storageQuotaBytesFrom = parseLongQueryOrNull(filterNames.STORAGE_QUOTA_BYTES_FROM),
        storageQuotaBytesTo = parseLongQueryOrNull(filterNames.STORAGE_QUOTA_BYTES_TO),
        isStorageQuotaBytesSet = parseBooleanQueryOrNull(filterNames.IS_STORAGE_QUOTA_BYTES_SET),
        storageMaxUploadSizeBytesFrom = parseLongQueryOrNull(filterNames.STORAGE_MAX_UPLOAD_SIZE_BYTES_FROM),
        storageMaxUploadSizeBytesTo = parseLongQueryOrNull(filterNames.STORAGE_MAX_UPLOAD_SIZE_BYTES_TO),
        isStorageMaxUploadSizeBytesSet = parseBooleanQueryOrNull(filterNames.IS_STORAGE_MAX_UPLOAD_SIZE_BYTES_SET),
        storageMaxPendingUploadsFrom = parseIntQueryOrNull(filterNames.STORAGE_MAX_PENDING_UPLOADS_FROM),
        storageMaxPendingUploadsTo = parseIntQueryOrNull(filterNames.STORAGE_MAX_PENDING_UPLOADS_TO),
        isStorageMaxPendingUploadsSet = parseBooleanQueryOrNull(filterNames.IS_STORAGE_MAX_PENDING_UPLOADS_SET),
        isStorageConfigurationSet = parseBooleanQueryOrNull(filterNames.IS_STORAGE_CONFIGURATION_SET),
        birthdateFrom = parseTimestampQueryOrNull(filterNames.BIRTHDATE_FROM),
        birthdateTo = parseTimestampQueryOrNull(filterNames.BIRTHDATE_TO),
        isBirthdateSet = parseBooleanQueryOrNull(filterNames.IS_BIRTHDATE_SET),
        searchQuery = firstNonBlankQueryValue(filterNames.SEARCH_QUERY)
    )
}
