package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import kotlinx.coroutines.CancellationException
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local filesystem implementation of [StorageService].
 *
 * Stores files under [StorageConfig.localStoragePath]. The optional bucket parameter is modeled as a subdirectory under the
 * root path. The constructor ensures that the root directory exists (creates it if missing).
 *
 * The public URL is built using [StorageConfig.s3PublicUrl] as a base, which allows using the same URL strategy across
 * local and S3 backends (e.g. through a reverse proxy).
 */
@Singleton
class LocalStorageService @Inject constructor(
    private val config: StorageConfig
) : StorageService {

    private val rootPath = Paths.get(config.localStoragePath).toAbsolutePath().normalize()

    init {
        if (!Files.exists(rootPath)) {
            Files.createDirectories(rootPath)
        }
    }

    override suspend fun save(
        fileName: String,
        content: ByteArray,
        contentType: String,
        bucket: String?
    ): AppResult<String> {
        return try {
            val targetDir = if (bucket != null) {
                rootPath.resolve(bucket)
            } else {
                rootPath
            }

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir)
            }

            val targetFile = targetDir.resolve(fileName)
            Files.write(targetFile, content)

            val relativeFileName = if (bucket != null) {
                "$bucket/$fileName"
            } else {
                fileName
            }
            AppResult.Success(relativeFileName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override suspend fun delete(key: String, bucket: String?): AppResult<Boolean> {
        return try {
            val fileToDelete = rootPath.resolve(key).toFile()
            val isExist = if (fileToDelete.exists()) {
                fileToDelete.delete()
            } else {
                false
            }
            AppResult.Success(isExist)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override suspend fun deleteBatch(keys: List<String>, bucket: String?): AppResult<Unit> {
        if (keys.isEmpty()) {
            return AppResult.Success(Unit)
        }
        return try {
            keys.forEach { key ->
                val fileToDelete = rootPath.resolve(key).toFile()
                if (fileToDelete.exists()) {
                    fileToDelete.delete()
                }
            }
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override fun getUrl(key: String): AppResult<String> {
        return AppResult.Success("${config.s3PublicUrl.removeSuffix("/")}/$key")
    }

    // todo implement methods
}