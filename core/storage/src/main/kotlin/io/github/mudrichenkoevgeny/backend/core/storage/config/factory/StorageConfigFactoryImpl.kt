package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys.StorageEnvKeys
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.enums.StorageType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageConfigFactoryImpl @Inject constructor(
    private val envReader: EnvReader
): StorageConfigFactory {

    override fun create(): StorageConfig {
        // secret files
        val s3AccessKeyFile = envReader.getByKey(StorageEnvKeys.S3_ACCESS_KEY_FILE)
        val s3SecretKeyFile = envReader.getByKey(StorageEnvKeys.S3_SECRET_KEY_FILE)

        // env
        val storageType = StorageType.fromString(envReader.getByKey(StorageEnvKeys.STORAGE_TYPE))
        val s3Endpoint = envReader.getByKey(StorageEnvKeys.S3_ENDPOINT)
        val s3Region = envReader.getByKey(StorageEnvKeys.S3_REGION)
        val s3BucketName = envReader.getByKey(StorageEnvKeys.S3_BUCKET_NAME)
        val s3PublicUrl = envReader.getByKey(StorageEnvKeys.S3_PUBLIC_URL)
        val forcePathStyle = envReader.getByKey(StorageEnvKeys.S3_FORCE_PATH_STYLE).toBoolean()
        val localStoragePath = envReader.getByKey(StorageEnvKeys.LOCAL_STORAGE_PATH)

        val s3AccessKey = envReader.readSecret(s3AccessKeyFile)
        val s3SecretKey = envReader.readSecret(s3SecretKeyFile)

        return StorageConfig(
            storageType = storageType,
            s3Endpoint = s3Endpoint,
            s3Region = s3Region,
            s3AccessKey = s3AccessKey,
            s3SecretKey = s3SecretKey,
            s3BucketName = s3BucketName,
            s3PublicUrl = s3PublicUrl,
            forcePathStyle = forcePathStyle,
            localStoragePath = localStoragePath
        )
    }
}