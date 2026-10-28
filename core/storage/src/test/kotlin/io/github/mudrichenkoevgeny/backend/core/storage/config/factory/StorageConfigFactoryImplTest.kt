package io.github.mudrichenkoevgeny.backend.core.storage.config.factory

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.storage.config.envkeys.StorageEnvKeys
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StorageConfigFactoryImplTest {

    private companion object {
        private const val S3_ACCESS_KEY_FILE = "/secrets/s3-access"
        private const val S3_SECRET_KEY_FILE = "/secrets/s3-secret"
        private const val S3_ENDPOINT = "https://s3.example.com"
        private const val S3_REGION = "us-east-1"
        private const val S3_BUCKET_NAME = "my-bucket"
        private const val S3_PUBLIC_URL = "https://cdn.example.com"
        private const val S3_ACCESS_KEY = "access-key"
        private const val S3_SECRET_KEY = "secret-key"
    }

    private val envReader = mockk<EnvReader>()

    private fun setupDefaultStorageEnvMocks() {
        every { envReader.getByKey(StorageEnvKeys.STORAGE_MIN_UPLOAD_SIZE_BYTES) } returns "1"
        every { envReader.getByKey(StorageEnvKeys.STORAGE_MAX_UPLOAD_SIZE_BYTES) } returns "52428800"
        every { envReader.getByKey(StorageEnvKeys.STORAGE_TOTAL_QUOTA_OPEN_USER_BYTES) } returns "1073741824"
        every { envReader.getByKey(StorageEnvKeys.STORAGE_TOTAL_QUOTA_MANAGEMENT_USER_BYTES) } returns ""
        every { envReader.getByKey(StorageEnvKeys.STORAGE_PRESIGNED_URL_EXPIRATION_SECONDS) } returns "900"
        every { envReader.getByKey(StorageEnvKeys.STORAGE_MAX_PENDING_UPLOADS_OPEN_USER) } returns "10"
        every { envReader.getByKey(StorageEnvKeys.STORAGE_MAX_PENDING_UPLOADS_MANAGEMENT_USER) } returns "100"
        every { envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_IS_DEDUPLICATION_ENABLED) } returns "true"
        every { envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_DELETED_FILES_CLEANUP_INTERVAL_SECONDS) } returns "86400"
        every { envReader.getByKeyOrNull(StorageEnvKeys.STORAGE_MAINTENANCE_BATCH_SIZE) } returns "100"
    }

    @Test
    fun `create builds StorageConfig from env and secret files`() {
        setupDefaultStorageEnvMocks()
        every { envReader.getByKey(StorageEnvKeys.S3_ACCESS_KEY_FILE) } returns S3_ACCESS_KEY_FILE
        every { envReader.getByKey(StorageEnvKeys.S3_SECRET_KEY_FILE) } returns S3_SECRET_KEY_FILE
        every { envReader.getByKey(StorageEnvKeys.S3_ENDPOINT) } returns S3_ENDPOINT
        every { envReader.getByKey(StorageEnvKeys.S3_REGION) } returns S3_REGION
        every { envReader.getByKey(StorageEnvKeys.S3_BUCKET_NAME) } returns S3_BUCKET_NAME
        every { envReader.getByKey(StorageEnvKeys.S3_PUBLIC_URL) } returns S3_PUBLIC_URL
        every { envReader.getByKey(StorageEnvKeys.S3_FORCE_PATH_STYLE) } returns "true"
        every { envReader.readSecret(S3_ACCESS_KEY_FILE) } returns S3_ACCESS_KEY
        every { envReader.readSecret(S3_SECRET_KEY_FILE) } returns S3_SECRET_KEY

        val factory = StorageConfigFactoryImpl(envReader)

        val config: StorageConfig = factory.create()

        assertEquals(S3_ENDPOINT, config.s3Endpoint)
        assertEquals(S3_REGION, config.s3Region)
        assertEquals(S3_ACCESS_KEY, config.s3AccessKey)
        assertEquals(S3_SECRET_KEY, config.s3SecretKey)
        assertEquals(S3_BUCKET_NAME, config.s3BucketName)
        assertEquals(S3_PUBLIC_URL, config.s3PublicUrl)
        assertTrue(config.forcePathStyle)
        assertEquals(100, config.maintenanceBatchSize)
        assertEquals(86400, config.managementStorageSettings.deletedFilesCleanupIntervalSeconds)
    }
}
