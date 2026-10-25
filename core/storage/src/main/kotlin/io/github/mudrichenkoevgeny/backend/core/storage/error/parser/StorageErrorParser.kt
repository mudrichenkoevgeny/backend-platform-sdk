package io.github.mudrichenkoevgeny.backend.core.storage.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.formatting.ByteSizeFormatter
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.error.naming.StorageErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.error.naming.StorageErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.localization.StorageModuleStrings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for storage-related errors.
 */
@Singleton
class StorageErrorParser @Inject constructor() : AppErrorSpecificParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse? {
        val strings = StorageModuleStrings(locale = locale).errors

        val message = when (code) {
            StorageErrorCodes.FILE_SIZE_EXCEEDS_LIMIT -> {
                val maxBytes = args?.get(StorageErrorArgs.MAX_FILE_SIZE_BYTES)
                val formatted = ByteSizeFormatter.format(maxBytes, locale = locale)
                if (formatted != null) {
                    strings.fileSizeExceedsLimit(formatted)
                } else {
                    strings.fileSizeExceedsLimitWithoutArgs
                }
            }

            StorageErrorCodes.FILE_SIZE_BELOW_MINIMUM -> {
                val minBytes = args?.get(StorageErrorArgs.MIN_FILE_SIZE_BYTES)
                val formatted = ByteSizeFormatter.format(minBytes, locale = locale)
                if (formatted != null) {
                    strings.fileSizeBelowMinimum(formatted)
                } else {
                    strings.fileSizeBelowMinimumWithoutArgs
                }
            }

            StorageErrorCodes.UNSUPPORTED_MIME_TYPE -> strings.unsupportedMimeType
            StorageErrorCodes.UNSUPPORTED_STORAGE_CATEGORY -> strings.unsupportedMimeType

            StorageErrorCodes.STORAGE_QUOTA_EXCEEDED -> {
                val usedBytes = args?.get(StorageErrorArgs.USED_BYTES)
                val quotaBytes = args?.get(StorageErrorArgs.QUOTA_BYTES)
                val formattedUsed = ByteSizeFormatter.format(usedBytes, locale = locale)
                val formattedQuota = ByteSizeFormatter.format(quotaBytes, locale = locale)
                if (formattedUsed != null && formattedQuota != null) {
                    strings.storageQuotaExceeded(formattedUsed = formattedUsed, formattedQuota = formattedQuota)
                } else {
                    strings.storageQuotaExceededWithoutArgs
                }
            }

            StorageErrorCodes.TOO_MANY_PENDING_UPLOADS -> {
                val maxPendingUploads = args?.get(StorageErrorArgs.MAX_PENDING_UPLOADS)
                if (maxPendingUploads != null) {
                    strings.tooManyPendingUploads(maxPendingUploads)
                } else {
                    strings.tooManyPendingUploadsWithoutArgs
                }
            }

            StorageErrorCodes.PRESIGNED_URL_EXPIRED -> strings.presignedUrlExpired
            StorageErrorCodes.INVALID_UPLOAD_TOKEN -> strings.uploadFailed
            StorageErrorCodes.UPLOAD_ALREADY_COMPLETED -> strings.uploadAlreadyCompleted
            StorageErrorCodes.STORAGE_UPLOAD_FAILED -> strings.storageUploadFailed
            StorageErrorCodes.STORAGE_ERROR -> strings.storageError
            StorageErrorCodes.FILE_NOT_FOUND -> strings.fileNotFound
            StorageErrorCodes.FILE_NOT_READY -> strings.fileNotReady
            StorageErrorCodes.FILE_ALREADY_EXISTS -> strings.fileAlreadyExists
            StorageErrorCodes.FILE_DELETE_FAILED -> strings.fileDeleteFailed
            else -> return null
        }

        return ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = code,
            message = message,
            args = args?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    override fun getApiErrorResponse(
        appError: AppError,
        locale: String
    ): ApiErrorResponse? {
        return getApiErrorResponse(
            errorId = appError.errorId,
            code = appError.code,
            args = appError.publicArgs,
            locale = locale
        )
    }
}