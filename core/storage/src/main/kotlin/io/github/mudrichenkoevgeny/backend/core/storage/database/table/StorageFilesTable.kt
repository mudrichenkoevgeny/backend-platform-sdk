package io.github.mudrichenkoevgeny.backend.core.storage.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.core.storage.database.StorageDbConstraints
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import org.jetbrains.exposed.v1.core.Column

/**
 * Storage files table storing metadata for uploaded media and document files.
 *
 * Schema is created by a Flyway migration in `db/migration/core/storage/`.
 * The app must include this path in its Flyway migration locations.
 */
public object StorageFilesTable : BaseTable("storage_files") {
    public val ownerUserId: Column<String?> = varchar("owner_user_id", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    public val fileName: Column<String> = varchar("file_name", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    public val fileSizeBytes: Column<Long> = long("file_size_bytes")
    public val mimeType: Column<String> = varchar("mime_type", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    public val category: Column<StorageCategory> = enumerationByName(
        "category",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        StorageCategory::class
    )
    public val visibility: Column<StorageFileVisibility> = enumerationByName(
        "visibility",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        StorageFileVisibility::class
    )
    public val status: Column<StorageFileStatus> = enumerationByName(
        "status",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        StorageFileStatus::class
    )
    public val targetScope: Column<String?> = varchar("target_scope", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    public val contentHash: Column<String?> = varchar("content_hash", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    public val s3Bucket: Column<String> = varchar("s3_bucket", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    public val s3Key: Column<String> = varchar("s3_key", StorageDbConstraints.S3_KEY_MAX_LENGTH)
}
