package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig

/**
 * Abstraction over object storage used by the SDK.
 *
 * Implementations may store objects on a local filesystem ([LocalStorageService]) or in S3-compatible storage ([S3StorageService]).
 * The default implementation is selected by DI based on [StorageConfig.storageType].
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
     * @return [AppResult.Success] with the stored key, or [AppResult.Error] on failure.
     */
    suspend fun save(
        fileName: String,
        content: ByteArray,
        contentType: String,
        bucket: String? = null
    ): AppResult<String>

    /**
     * Deletes an object by key.
     *
     * @param key object key to delete.
     * @param bucket optional bucket override (used by S3 implementation); when `null`, the default bucket from config is used.
     * @return [AppResult.Success] with `true` when deletion was successful, otherwise `false`; or [AppResult.Error] on failure.
     */
    suspend fun delete(key: String, bucket: String? = null): AppResult<Boolean>

    /**
     * Builds a public URL for the given key (without performing I/O).
     *
     * @param key object key.
     * @return [AppResult.Success] with an URL string, or [AppResult.Error] on failure.
     */
    fun getUrl(key: String): AppResult<String>
}