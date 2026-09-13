package io.github.mudrichenkoevgeny.backend.core.storage.config.model

import io.github.mudrichenkoevgeny.backend.core.storage.model.StorageType

fun createTestStorageConfig(
    storageType: StorageType = StorageType.LOCAL,
    s3Endpoint: String = "",
    s3Region: String = "",
    s3AccessKey: String = "",
    s3SecretKey: String = "",
    s3BucketName: String = "",
    s3PublicUrl: String = "https://example.com/files",
    forcePathStyle: Boolean = false,
    localStoragePath: String = "/tmp/storage"
) = StorageConfig(
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
