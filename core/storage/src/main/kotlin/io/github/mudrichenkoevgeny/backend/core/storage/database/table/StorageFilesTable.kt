package io.github.mudrichenkoevgeny.backend.core.storage.database.table

import io.github.mudrichenkoevgeny.backend.core.database.BaseDbConstraints
import io.github.mudrichenkoevgeny.backend.core.database.table.BaseTable
import io.github.mudrichenkoevgeny.backend.core.storage.database.StorageDbConstraints
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility

/**
 * Storage files table storing metadata for uploaded media and document files.
 *
 * Schema is created by a Flyway migration in `db/migration/core/storage/`.
 * The app must include this path in its Flyway migration locations.
 */
object StorageFilesTable : BaseTable("storage_files") {
    val ownerUserId = varchar("owner_user_id", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val fileName = varchar("file_name", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val fileSizeBytes = long("file_size_bytes")
    val mimeType = varchar("mime_type", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val category = enumerationByName(
        "category",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        StorageCategory::class
    )
    val visibility = enumerationByName(
        "visibility",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        StorageFileVisibility::class
    )
    val status = enumerationByName(
        "status",
        BaseDbConstraints.ENUM_MAX_LENGTH,
        StorageFileStatus::class
    )
    val targetScope = varchar("target_scope", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val contentHash = varchar("content_hash", BaseDbConstraints.DEFAULT_MAX_LENGTH).nullable()
    val s3Bucket = varchar("s3_bucket", BaseDbConstraints.DEFAULT_MAX_LENGTH)
    val s3Key = varchar("s3_key", StorageDbConstraints.S3_KEY_MAX_LENGTH)
}