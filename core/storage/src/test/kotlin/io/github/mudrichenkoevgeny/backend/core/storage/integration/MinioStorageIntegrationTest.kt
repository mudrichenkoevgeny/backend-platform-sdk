package io.github.mudrichenkoevgeny.backend.core.storage.integration

import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.testing.docker.RequiresDocker
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.createTestStorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.error.model.StorageError
import io.github.mudrichenkoevgeny.backend.core.storage.imgproxy.ImgproxyUrlBuilder
import io.github.mudrichenkoevgeny.backend.core.storage.service.S3StorageService
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI

@RequiresDocker
internal class MinioStorageIntegrationTest {
    @Test
    fun `s3 storage service should perform object lifecycle operations on minio`() {
        val minioImage = DockerImageName.parse(MINIO_IMAGE).asCompatibleSubstituteFor("minio/minio")
        MinIOContainer(minioImage).use { minio ->
            minio.withUserName(ACCESS_KEY)
            minio.withPassword(SECRET_KEY)
            minio.start()
            assertTrue(minio.isRunning)

            val storageConfig = createTestStorageConfig(
                s3Endpoint = minio.s3URL,
                s3Region = S3_REGION,
                s3AccessKey = ACCESS_KEY,
                s3SecretKey = SECRET_KEY,
                s3BucketNamePublic = PUBLIC_BUCKET,
                s3BucketNamePrivate = PRIVATE_BUCKET,
                s3PublicUrl = minio.s3URL,
                forcePathStyle = true,
                maintenanceBatchSize = 100
            )

            val imgproxyUrlBuilder = mockk<ImgproxyUrlBuilder>(relaxed = true)
            val appLogger = mockk<AppLogger>(relaxed = true)

            val storageService = S3StorageService(
                config = storageConfig,
                imgproxyUrlBuilder = imgproxyUrlBuilder,
                appLogger = appLogger
            )

            val payloadBytes = TEST_PAYLOAD.toByteArray()

            runBlocking {
                val ensurePublicResult = storageService.ensureBucketExists(
                    bucket = PUBLIC_BUCKET,
                    isPublic = true
                )
                assertTrue(ensurePublicResult is AppResult.Success)

                val ensurePrivateResult = storageService.ensureBucketExists(
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                assertTrue(ensurePrivateResult is AppResult.Success)

                val saveResult = storageService.save(
                    fileName = TEST_FILE_NAME,
                    content = payloadBytes,
                    contentType = CONTENT_TYPE,
                    bucket = PUBLIC_BUCKET,
                    isPublic = true
                )
                assertTrue(saveResult is AppResult.Success)
                assertEquals(TEST_FILE_NAME, (saveResult as AppResult.Success).data)

                val metadataResult = storageService.getObjectMetadata(
                    key = TEST_FILE_NAME,
                    bucket = PUBLIC_BUCKET,
                    isPublic = true
                )
                assertTrue(metadataResult is AppResult.Success)
                val metadata = (metadataResult as AppResult.Success).data
                assertEquals(payloadBytes.size.toLong(), metadata.sizeBytes)
                assertTrue(metadata.eTag?.isNotBlank() == true)

                val presignedPutResult = storageService.generatePresignedPutUrl(
                    key = "presigned-upload.txt",
                    contentType = CONTENT_TYPE,
                    expirationSeconds = 3600,
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                assertTrue(presignedPutResult is AppResult.Success)
                assertTrue((presignedPutResult as AppResult.Success).data.startsWith("http"))

                val presignedGetResult = storageService.generatePresignedGetUrl(
                    key = TEST_FILE_NAME,
                    expirationSeconds = 3600,
                    bucket = PUBLIC_BUCKET,
                    isPublic = true
                )
                assertTrue(presignedGetResult is AppResult.Success)
                assertTrue((presignedGetResult as AppResult.Success).data.startsWith("http"))

                val publicUrlResult = storageService.getPublicUrl(
                    key = TEST_FILE_NAME,
                    bucket = PUBLIC_BUCKET
                )
                assertTrue(publicUrlResult is AppResult.Success)
                val publicUrl = (publicUrlResult as AppResult.Success).data
                assertTrue(publicUrl.contains(PUBLIC_BUCKET) && publicUrl.contains(TEST_FILE_NAME))

                val downloadedBytes = withContext(Dispatchers.IO) {
                    URI.create(publicUrl).toURL().readBytes()
                }
                assertTrue(payloadBytes.contentEquals(downloadedBytes))

                val deleteResult = storageService.delete(
                    key = TEST_FILE_NAME,
                    bucket = PUBLIC_BUCKET,
                    isPublic = true
                )
                assertTrue(deleteResult is AppResult.Success)

                val metadataAfterDelete = storageService.getObjectMetadata(
                    key = TEST_FILE_NAME,
                    bucket = PUBLIC_BUCKET,
                    isPublic = true
                )
                assertTrue(metadataAfterDelete is AppResult.Error)
                assertTrue((metadataAfterDelete as AppResult.Error).error is StorageError.FileNotFound)

                val batchSave1 = storageService.save(
                    fileName = BATCH_FILE_1,
                    content = payloadBytes,
                    contentType = CONTENT_TYPE,
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                val batchSave2 = storageService.save(
                    fileName = BATCH_FILE_2,
                    content = payloadBytes,
                    contentType = CONTENT_TYPE,
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                assertTrue((batchSave1 is AppResult.Success) && (batchSave2 is AppResult.Success))

                val batchDeleteResult = storageService.deleteBatch(
                    keys = listOf(BATCH_FILE_1, BATCH_FILE_2),
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                assertTrue(batchDeleteResult is AppResult.Success)

                val batch1Metadata = storageService.getObjectMetadata(
                    key = BATCH_FILE_1,
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                assertTrue(batch1Metadata is AppResult.Error)
                assertTrue((batch1Metadata as AppResult.Error).error is StorageError.FileNotFound)

                val batch2Metadata = storageService.getObjectMetadata(
                    key = BATCH_FILE_2,
                    bucket = PRIVATE_BUCKET,
                    isPublic = false
                )
                assertTrue(batch2Metadata is AppResult.Error)
                assertTrue((batch2Metadata as AppResult.Error).error is StorageError.FileNotFound)
            }
        }
    }

    private companion object {
        private const val MINIO_IMAGE = "elestio/minio:latest"
        private const val ACCESS_KEY = "minioadmin"
        private const val SECRET_KEY = "minioadmin"
        private const val PUBLIC_BUCKET = "test-public-bucket"
        private const val PRIVATE_BUCKET = "test-private-bucket"
        private const val TEST_FILE_NAME = "integration-tests/sample-file.txt"
        private const val CONTENT_TYPE = "text/plain"
        private const val TEST_PAYLOAD = "Hello MinIO Integration Test!"
        private const val S3_REGION = "us-east-1"
        private const val BATCH_FILE_1 = "batch/file1.txt"
        private const val BATCH_FILE_2 = "batch/file2.txt"
    }
}
