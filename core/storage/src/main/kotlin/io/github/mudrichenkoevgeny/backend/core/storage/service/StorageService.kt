package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.domain.model.S3ObjectMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileUrls
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal

/**
 * Abstraction over S3-compatible object storage used by the SDK.
 *
 * Implemented by [S3StorageService] using AWS SDK.
 *
 * Methods return [AppResult] to provide a consistent error model across SDK modules.
 */
interface StorageService {

    /**
     * Saves a file to storage and returns its storage key.
     *
     * @param fileName object name (key) to store under.
     * @param content file bytes.
     * @param contentType MIME type (e.g. `image/png`).
     * @param bucket optional bucket override (used by S3 implementation); when `null`, the default bucket from config is used.
     * @param isPublic indicates if the file should be stored in the public bucket.
     * @return [AppResult.Success] with the stored key, or [AppResult.Error] on failure.
     */
    suspend fun save(
        fileName: String,
        content: ByteArray,
        contentType: String,
        bucket: String? = null,
        isPublic: Boolean = false
    ): AppResult<String>

    /**
     * Deletes an object by key.
     *
     * @param key object key to delete.
     * @param bucket optional bucket override (used by S3 implementation); when `null`, the default bucket from config is used.
     * @param isPublic indicates if the file is in the public bucket.
     * @return [AppResult.Success] with `true` when deletion was successful, otherwise `false`; or [AppResult.Error] on failure.
     */
    suspend fun delete(key: String, bucket: String? = null, isPublic: Boolean = false): AppResult<Boolean>

    /**
     * Deletes multiple objects by keys in a single batch operation.
     *
     * @param keys list of object keys to delete.
     * @param bucket optional bucket override; when `null`, the default bucket from config is used.
     * @param isPublic indicates if the files are in the public bucket.
     * @return [AppResult.Success] on successful batch deletion, or [AppResult.Error] on failure.
     */
    suspend fun deleteBatch(keys: List<String>, bucket: String? = null, isPublic: Boolean = false): AppResult<Unit> =
        AppResult.Error(CommonError.Internal(UnsupportedOperationException("deleteBatch is not implemented")))

    /**
     * Builds a direct public URL for the given key in the public bucket (without performing I/O).
     *
     * @param key object key.
     * @param bucket optional bucket name override; when null, default public bucket from configuration is used.
     * @return [AppResult.Success] with the public URL string, or [AppResult.Error] on failure.
     */
    fun getPublicUrl(key: String, bucket: String? = null): AppResult<String>

    /**
     * Generates a short-lived presigned URL for direct client upload (PUT).
     */
    fun generatePresignedPutUrl(
        key: String,
        contentType: String,
        expirationSeconds: Int,
        bucket: String? = null,
        isPublic: Boolean = false
    ): AppResult<String> = AppResult.Error(CommonError.Internal(UnsupportedOperationException("generatePresignedPutUrl is not implemented")))

    /**
     * Generates a short-lived presigned URL for direct client download (GET).
     */
    fun generatePresignedGetUrl(
        key: String,
        expirationSeconds: Int,
        bucket: String? = null,
        isPublic: Boolean = false
    ): AppResult<String> = AppResult.Error(CommonError.Internal(UnsupportedOperationException("generatePresignedGetUrl is not implemented")))

    /**
     * Retrieves object metadata (including size in bytes and content hash/ETag) from the storage provider.
     *
     * @param key Target object key in storage.
     * @param bucket Optional bucket name override; uses the default bucket when null.
     * @param isPublic indicates if the file is in the public bucket.
     * @return [AppResult] containing [S3ObjectMetadata] on success or an error on failure.
     */
    suspend fun getObjectMetadata(key: String, bucket: String? = null, isPublic: Boolean = false): AppResult<S3ObjectMetadata> =
        AppResult.Error(CommonError.Internal(UnsupportedOperationException("getObjectMetadata is not implemented")))

    /**
     * Resolves structured URLs ([FileUrls]) for a given storage file, including presigned GET URLs
     * for non-public files and signed imgproxy URLs for thumbnails and previews.
     *
     * @param file target internal storage file record.
     * @return [FileUrls] containing original, thumbnail, and preview URLs.
     */
    fun resolveFileUrls(file: StorageFileInternal): FileUrls

    /**
     * Ensures that the specified target bucket exists in storage, creating it if necessary.
     *
     * @param bucket Target bucket name override; when null, default bucket from configuration is used.
     * @param isPublic indicates if the public bucket should be ensured.
     * @return [AppResult.Success] when the bucket exists or was successfully created, otherwise [AppResult.Error].
     */
    suspend fun ensureBucketExists(bucket: String? = null, isPublic: Boolean = false): AppResult<Unit> = AppResult.Success(Unit)
}