package io.github.mudrichenkoevgeny.backend.feature.user.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.json.jsonb
import java.time.Instant
import kotlin.uuid.Uuid

/**
 * Users table.
 *
 * Schema is created by a Flyway migration in `db/migration/feature/user/`.
 * The app must include this path in its Flyway migration locations.
 */
public object UsersTable : BaseTable("users") {
    public const val NICKNAME_UNIQUE_INDEX_NAME: String = "idx_users_nickname"

    public val role: Column<UserRole> = enumerationByName(
        "role",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserRole::class
    )
    public val accountStatus: Column<UserAccountStatus> = enumerationByName(
        "account_status",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAccountStatus::class
    )
    public val accountStatusOnRestore: Column<UserAccountStatus?> = enumerationByName(
        "account_status_on_restore",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        UserAccountStatus::class
    ).nullable()
    public val authorityLevel: Column<Int> = integer("authority_level").default(0)
    public val permissionCodes: Column<Set<String>> = jsonb<Set<String>>(
        "permission_codes",
        FoundationJson,
        serializer<Set<String>>()
    )
    public val isTotpEnabled: Column<Boolean> = bool("is_totp_enabled").default(false)
    public val accountLockoutType: Column<AccountLockoutType> = enumerationByName(
        "account_lockout_type",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        AccountLockoutType::class
    ).default(AccountLockoutType.NONE)
    public val temporaryLockoutUntil: Column<Instant?> = timestamp("temporary_lockout_until").nullable()
    public val storageQuotaBytes: Column<Long?> = long("storage_quota_bytes").nullable()
    public val storageMaxUploadSizeBytes: Column<Long?> = long("storage_max_upload_size_bytes").nullable()
    public val storageMaxPendingUploads: Column<Int?> = integer("storage_max_pending_uploads").nullable()
    public val lastLoginAt: Column<Instant?> = timestamp("last_login_at").nullable()
    public val lastActiveAt: Column<Instant?> = timestamp("last_active_at").nullable()
    public val scheduledPermanentDeletionAt: Column<Instant?> = timestamp("scheduled_permanent_deletion_at").nullable()
    public val nickname: Column<String?> = varchar(
        "nickname",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val displayName: Column<String?> = varchar(
        "display_name",
        BaseDbConstraints.DEFAULT_MAX_LENGTH
    ).nullable()
    public val avatarFileId: Column<Uuid?> = uuid("avatar_file_id").nullable()
    public val birthdate: Column<Instant?> = timestamp("birthdate").nullable()
}
