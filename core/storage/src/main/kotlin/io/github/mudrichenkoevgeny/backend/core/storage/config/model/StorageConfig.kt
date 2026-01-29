package io.github.mudrichenkoevgeny.backend.core.storage.config.model

import io.github.mudrichenkoevgeny.backend.core.storage.enums.StorageType

data class StorageConfig(
    val storageType: StorageType,
    val s3Endpoint: String,
    val s3Region: String,
    val s3AccessKey: String,
    val s3SecretKey: String,
    val s3BucketName: String,
    val s3PublicUrl: String,
    val forcePathStyle: Boolean,
    val localStoragePath: String
)