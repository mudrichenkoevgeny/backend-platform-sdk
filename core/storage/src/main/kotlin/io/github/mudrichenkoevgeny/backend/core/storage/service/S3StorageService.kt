package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.domain.model.S3ObjectMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * S3-backed implementation of [StorageService] using AWS SDK v2 async client and presigner.
 *
 * Configured using [StorageConfig] for endpoint, region, access credentials, bucket name, and public URL.
 * Upload, delete, and head object operations execute asynchronously via [S3AsyncClient] and [S3Presigner].
 *
 * @param config Configuration parameters holding S3 credentials, endpoint, and bucket options.
 */
@Singleton
class S3StorageService @Inject constructor(
    private val config: StorageConfig
) : StorageService {

    private val basicCredentials = AwsBasicCredentials.create(config.s3AccessKey, config.s3SecretKey)

    private val s3Client = S3AsyncClient.builder()
        .endpointOverride(URI.create(config.s3Endpoint))
        .region(Region.of(config.s3Region))
        .credentialsProvider(
            StaticCredentialsProvider.create(basicCredentials)
        )
        .forcePathStyle(config.forcePathStyle)
        .build()

    private val presigner = S3Presigner.builder()
        .endpointOverride(URI.create(config.s3Endpoint))
        .region(Region.of(config.s3Region))
        .credentialsProvider(
            StaticCredentialsProvider.create(basicCredentials)
        )
        .build()

    override suspend fun save(
        fileName: String,
        content: ByteArray,
        contentType: String,
        bucket: String?
    ): AppResult<String> {
        val targetBucket = bucket ?: config.s3BucketName

        val request = PutObjectRequest.builder()
            .bucket(targetBucket)
            .key(fileName)
            .contentType(contentType)
            .build()

        return try {
            s3Client.putObject(request, AsyncRequestBody.fromBytes(content)).await()
            AppResult.Success(fileName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override suspend fun delete(key: String, bucket: String?): AppResult<Boolean> {
        val targetBucket = bucket ?: config.s3BucketName

        val request = DeleteObjectRequest.builder()
            .bucket(targetBucket)
            .key(key)
            .build()

        return try {
            val response = s3Client.deleteObject(request).await()
            val isSuccessful = response.sdkHttpResponse().isSuccessful
            AppResult.Success(isSuccessful)
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
        val targetBucket = bucket ?: config.s3BucketName

        return try {
            val objectIdentifiers = keys.map { ObjectIdentifier.builder().key(it).build() }
            val delete = Delete.builder().objects(objectIdentifiers).build()
            val request = DeleteObjectsRequest.builder()
                .bucket(targetBucket)
                .delete(delete)
                .build()

            s3Client.deleteObjects(request).await()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override fun getUrl(key: String): AppResult<String> {
        val baseUrl = config.s3PublicUrl.removeSuffix("/")
        return AppResult.Success("$baseUrl/$key")
    }

    override fun generatePresignedPutUrl(
        key: String,
        contentType: String,
        expirationSeconds: Int,
        bucket: String?
    ): AppResult<String> {
        return try {
            val targetBucket = bucket ?: config.s3BucketName
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(targetBucket)
                .key(key)
                .contentType(contentType)
                .build()

            val presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds.toLong()))
                .putObjectRequest(putObjectRequest)
                .build()

            val presignedUrl = presigner.presignPutObject(presignRequest).url().toString()
            AppResult.Success(presignedUrl)
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override fun generatePresignedGetUrl(
        key: String,
        expirationSeconds: Int,
        bucket: String?
    ): AppResult<String> {
        return try {
            val targetBucket = bucket ?: config.s3BucketName
            val getObjectRequest = GetObjectRequest.builder()
                .bucket(targetBucket)
                .key(key)
                .build()

            val presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds.toLong()))
                .getObjectRequest(getObjectRequest)
                .build()

            val presignedUrl = presigner.presignGetObject(presignRequest).url().toString()
            AppResult.Success(presignedUrl)
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override suspend fun getObjectMetadata(key: String, bucket: String?): AppResult<S3ObjectMetadata> {
        return try {
            val targetBucket = bucket ?: config.s3BucketName
            val headRequest = HeadObjectRequest.builder()
                .bucket(targetBucket)
                .key(key)
                .build()
            val headResponse = s3Client.headObject(headRequest).await()

            AppResult.Success(
                S3ObjectMetadata(
                    sizeBytes = headResponse.contentLength(),
                    eTag = headResponse.eTag()?.trim('"')
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }
}