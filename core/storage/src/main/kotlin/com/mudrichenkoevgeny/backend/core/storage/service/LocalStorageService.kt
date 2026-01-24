package com.mudrichenkoevgeny.backend.core.storage.service

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import javax.inject.Singleton

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
            val targetDir = if (bucket != null) rootPath.resolve(bucket) else rootPath

            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir)
            }

            val targetFile = targetDir.resolve(fileName)
            Files.write(targetFile, content)

            val fileName = if (bucket != null) {
                "$bucket/$fileName"
            } else {
                fileName
            }
            AppResult.Success(fileName)
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
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
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override fun getUrl(key: String): AppResult<String> {
        return AppResult.Success("${config.s3PublicUrl.removeSuffix("/")}/$key")
    }
}