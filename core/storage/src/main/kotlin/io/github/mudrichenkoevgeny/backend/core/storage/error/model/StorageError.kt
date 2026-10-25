package io.github.mudrichenkoevgeny.backend.core.storage.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorSeverity
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileId
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.error.naming.StorageErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.error.naming.StorageErrorCodes
import io.ktor.http.HttpStatusCode

/**
 * Storage feature errors: file upload limits, quotas, media validation, and S3 operations.
 *
 * Uses [publicArgs] for client-visible data; [secretArgs] for internal logs only.
 * Each variant has a stable [code] for i18n and a unique [errorId] for correlation.
 */
sealed class StorageError(
    override val errorId: ErrorId,
    override val code: String,
    override val publicArgs: Map<String, Any>? = null,
    override val secretArgs: Map<String, Any>? = null,
    override val httpStatusCode: HttpStatusCode,
    override val appErrorSeverity: AppErrorSeverity
) : AppError {

    /**
     * File size exceeds the maximum permitted limit for its category or system policy.
     *
     * @param fileSizeBytes Size of the file in bytes.
     * @param maxFileSizeBytes Maximum permitted file size in bytes.
     * @param category Optional storage category identifier.
     * @param fileName Optional original file name.
     */
    class FileSizeExceedsLimit(
        fileSizeBytes: Long,
        maxFileSizeBytes: Long,
        category: String? = null,
        fileName: String? = null
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.FILE_SIZE_EXCEEDS_LIMIT,
        publicArgs = buildMap {
            put(StorageErrorArgs.FILE_SIZE_BYTES, fileSizeBytes.toString())
            put(StorageErrorArgs.MAX_FILE_SIZE_BYTES, maxFileSizeBytes.toString())
            category?.let { put(StorageErrorArgs.CATEGORY, it) }
            fileName?.let { put(StorageErrorArgs.FILE_NAME, it) }
        },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * File size is below the minimum permitted threshold.
     *
     * @param fileSizeBytes Size of the file in bytes.
     * @param minFileSizeBytes Minimum permitted file size in bytes.
     * @param category Optional storage category identifier.
     * @param fileName Optional original file name.
     */
    class FileSizeBelowMinimum(
        fileSizeBytes: Long,
        minFileSizeBytes: Long,
        category: String? = null,
        fileName: String? = null
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.FILE_SIZE_BELOW_MINIMUM,
        publicArgs = buildMap {
            put(StorageErrorArgs.FILE_SIZE_BYTES, fileSizeBytes.toString())
            put(StorageErrorArgs.MIN_FILE_SIZE_BYTES, minFileSizeBytes.toString())
            category?.let { put(StorageErrorArgs.CATEGORY, it) }
            fileName?.let { put(StorageErrorArgs.FILE_NAME, it) }
        },
        httpStatusCode = HttpStatusCode.BadRequest,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Provided MIME type is not supported for the target storage category.
     *
     * @param mimeType Provided MIME type.
     * @param category Optional storage category identifier.
     * @param fileName Optional original file name.
     */
    class UnsupportedMimeType(
        mimeType: String,
        category: String? = null,
        fileName: String? = null
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.UNSUPPORTED_MIME_TYPE,
        publicArgs = buildMap {
            put(StorageErrorArgs.MIME_TYPE, mimeType)
            category?.let { put(StorageErrorArgs.CATEGORY, it) }
            fileName?.let { put(StorageErrorArgs.FILE_NAME, it) }
        },
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Category is invalid or unsupported.
     *
     * @param category Provided category string.
     */
    class UnsupportedStorageCategory(
        category: String
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.UNSUPPORTED_STORAGE_CATEGORY,
        publicArgs = mapOf(
            StorageErrorArgs.CATEGORY to category
        ),
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * User has exceeded their total storage quota.
     *
     * @param quotaBytes Maximum permitted cumulative storage quota.
     * @param usedBytes Currently used storage size in bytes.
     */
    class StorageQuotaExceeded(
        quotaBytes: Long,
        usedBytes: Long
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.STORAGE_QUOTA_EXCEEDED,
        publicArgs = mapOf(
            StorageErrorArgs.QUOTA_BYTES to quotaBytes.toString(),
            StorageErrorArgs.USED_BYTES to usedBytes.toString()
        ),
        httpStatusCode = HttpStatusCode.Forbidden,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Too many pending unfinalized uploads.
     *
     * @param maxPendingUploads Maximum permitted active upload sessions.
     */
    class TooManyPendingUploads(
        maxPendingUploads: Int
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.TOO_MANY_PENDING_UPLOADS,
        publicArgs = mapOf(
            StorageErrorArgs.MAX_PENDING_UPLOADS to maxPendingUploads.toString()
        ),
        httpStatusCode = HttpStatusCode.TooManyRequests,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Presigned upload URL has expired.
     */
    class PresignedUrlExpired : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.PRESIGNED_URL_EXPIRED,
        httpStatusCode = HttpStatusCode.Gone,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Upload session token or signature is invalid.
     */
    class InvalidUploadToken : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.INVALID_UPLOAD_TOKEN,
        httpStatusCode = HttpStatusCode.Unauthorized,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Upload session or presigned URL has already been completed.
     *
     * @param fileId Target file ID.
     */
    class UploadAlreadyCompleted(
        fileId: FileId
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.UPLOAD_ALREADY_COMPLETED,
        secretArgs = mapOf(
            StorageErrorArgs.FILE_ID to fileId.asHexDashString()
        ),
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * Upload process or verification failed.
     *
     * @param fileId Optional target file ID.
     * @param message Optional verification error message.
     */
    class StorageUploadFailed(
        fileId: FileId? = null,
        message: String? = null
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.STORAGE_UPLOAD_FAILED,
        secretArgs = buildMap {
            fileId?.let { put(StorageErrorArgs.FILE_ID, it.asHexDashString()) }
            message?.let { put(CommonErrorArgs.MESSAGE, it) }
        },
        httpStatusCode = HttpStatusCode.UnprocessableEntity,
        appErrorSeverity = AppErrorSeverity.MEDIUM
    )

    /**
     * Target file record was not found or access is forbidden.
     *
     * @param fileId Optional target file ID.
     */
    class FileNotFound(
        fileId: FileId? = null
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.FILE_NOT_FOUND,
        secretArgs = fileId?.let {
            mapOf(StorageErrorArgs.FILE_ID to it.asHexDashString())
        },
        httpStatusCode = HttpStatusCode.NotFound,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * File is not yet ready because it is still being processed.
     *
     * @param fileId Target file ID.
     */
    class FileNotReady(
        fileId: FileId
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.FILE_NOT_READY,
        secretArgs = mapOf(
            StorageErrorArgs.FILE_ID to fileId.asHexDashString()
        ),
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * File already exists and cannot be overwritten.
     *
     * @param fileId Target file ID.
     */
    class FileAlreadyExists(
        fileId: FileId
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.FILE_ALREADY_EXISTS,
        secretArgs = mapOf(
            StorageErrorArgs.FILE_ID to fileId.asHexDashString()
        ),
        httpStatusCode = HttpStatusCode.Conflict,
        appErrorSeverity = AppErrorSeverity.LOW
    )

    /**
     * File deletion failed in persistent storage.
     *
     * @param fileId Target file ID.
     */
    class FileDeleteFailed(
        fileId: FileId
    ) : StorageError(
        errorId = ErrorId.generate(),
        code = StorageErrorCodes.FILE_DELETE_FAILED,
        secretArgs = mapOf(
            StorageErrorArgs.FILE_ID to fileId.asHexDashString()
        ),
        httpStatusCode = HttpStatusCode.InternalServerError,
        appErrorSeverity = AppErrorSeverity.HIGH
    )
}