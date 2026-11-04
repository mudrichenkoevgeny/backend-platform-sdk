package io.github.mudrichenkoevgeny.backend.core.storage.service

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.dataOrNull
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.backend.core.storage.domain.model.S3ObjectMetadata
import io.github.mudrichenkoevgeny.backend.core.storage.error.model.StorageError
import io.github.mudrichenkoevgeny.backend.core.storage.imgproxy.ImgproxyUrlBuilder
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.FileUrls
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.file.StorageFileInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.status.StorageFileStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.visibility.StorageFileVisibility
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.Delete
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.ObjectIdentifier
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest
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
 * @param imgproxyUrlBuilder Service used to construct signed imgproxy thumbnail and preview URLs.
 * @param appLogger Application logger instance used for recording infrastructure errors.
 */
@Singleton
class S3StorageService @Inject constructor(
    private val config: StorageConfig,
    private val imgproxyUrlBuilder: ImgproxyUrlBuilder,
    private val appLogger: AppLogger
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

    private val presignerEndpoint: URI = run {
        val publicUrl = config.s3PublicUrl.removeSuffix("/")
        val uri = URI.create(publicUrl)
        val scheme = uri.scheme
        val authority = uri.authority ?: uri.host
        if (scheme != null && authority != null) {
            URI("$scheme://$authority")
        } else {
            uri
        }
    }

    private val presigner = S3Presigner.builder()
        .endpointOverride(presignerEndpoint)
        .region(Region.of(config.s3Region))
        .credentialsProvider(
            StaticCredentialsProvider.create(basicCredentials)
        )
        .serviceConfiguration(
            S3Configuration.builder()
                .pathStyleAccessEnabled(config.forcePathStyle)
                .build()
        )
        .build()

    override suspend fun save(
        fileName: String,
        content: ByteArray,
        contentType: String,
        bucket: String?,
        isPublic: Boolean
    ): AppResult<String> {
        val targetBucket = resolveTargetBucket(bucket, isPublic)

        val request = PutObjectRequest.builder()
            .bucket(targetBucket)
            .key(fileName)
            .contentType(contentType)
            .cacheControl(IMMUTABLE_CACHE_CONTROL_HEADER)
            .build()

        return try {
            s3Client.putObject(request, AsyncRequestBody.fromBytes(content)).await()
            AppResult.Success(fileName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: NoSuchBucketException) {
            try {
                appLogger.logError(CommonError.Internal(e))
                s3Client.createBucket(CreateBucketRequest.builder().bucket(targetBucket).build()).await()
                s3Client.putObject(request, AsyncRequestBody.fromBytes(content)).await()
                AppResult.Success(fileName)
            } catch (retryException: CancellationException) {
                throw retryException
            } catch (retryException: Exception) {
                AppResult.Error(CommonError.Internal(retryException))
            }
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override suspend fun delete(key: String, bucket: String?, isPublic: Boolean): AppResult<Boolean> {
        val targetBucket = resolveTargetBucket(bucket, isPublic)

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
        } catch (_: NoSuchBucketException) {
            AppResult.Success(true)
        } catch (_: NoSuchKeyException) {
            AppResult.Success(true)
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override suspend fun deleteBatch(keys: List<String>, bucket: String?, isPublic: Boolean): AppResult<Unit> {
        if (keys.isEmpty()) {
            return AppResult.Success(Unit)
        }
        val targetBucket = resolveTargetBucket(bucket, isPublic)

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
        } catch (_: NoSuchBucketException) {
            AppResult.Success(Unit)
        } catch (_: NoSuchKeyException) {
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override fun getPublicUrl(key: String, bucket: String?): AppResult<String> {
        val targetBucket = bucket ?: config.s3BucketNamePublic
        return AppResult.Success("${config.s3PublicUrl.removeSuffix("/")}/$targetBucket/$key")
    }

    override fun generatePresignedPutUrl(
        key: String,
        contentType: String,
        expirationSeconds: Int,
        bucket: String?,
        isPublic: Boolean
    ): AppResult<String> {
        return try {
            val targetBucket = resolveTargetBucket(bucket, isPublic)
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
        bucket: String?,
        isPublic: Boolean
    ): AppResult<String> {
        return try {
            val targetBucket = resolveTargetBucket(bucket, isPublic)
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

    override suspend fun getObjectMetadata(key: String, bucket: String?, isPublic: Boolean): AppResult<S3ObjectMetadata> {
        return try {
            val targetBucket = resolveTargetBucket(bucket, isPublic)
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
        } catch (_: NoSuchKeyException) {
            AppResult.Error(StorageError.FileNotFound())
        } catch (_: NoSuchBucketException) {
            AppResult.Error(StorageError.FileNotFound())
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    override fun resolveFileUrls(file: StorageFileInternal): FileUrls {
        val isPublic = file.visibility == StorageFileVisibility.PUBLIC
        val isPublicBucket = file.s3Bucket == config.s3BucketNamePublic

        val originalUrl = if (isPublic && isPublicBucket) {
            "${config.s3PublicUrl.removeSuffix("/")}/${file.s3Bucket}/${file.s3Key}"
        } else {
            generatePresignedGetUrl(
                key = file.s3Key,
                expirationSeconds = PRIVATE_FILE_PRESIGNED_URL_EXPIRATION_SECONDS,
                bucket = file.s3Bucket,
                isPublic = false
            ).dataOrNull().orEmpty()
        }

        val urlPresets = if (file.status == StorageFileStatus.READY) {
            imgproxyUrlBuilder.buildPresetUrls(
                bucket = file.s3Bucket,
                key = file.s3Key,
                mimeType = file.mimeType,
                category = file.category
            )
        } else {
            emptyMap()
        }

        return FileUrls(
            originalUrl = originalUrl,
            urlPresets = urlPresets
        )
    }

    override suspend fun ensureBucketExists(bucket: String?, isPublic: Boolean): AppResult<Unit> {
        val targetBucket = resolveTargetBucket(bucket, isPublic)
        return try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(targetBucket).build()).await()
            AppResult.Success(Unit)
        } catch (_: NoSuchBucketException) {
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(targetBucket).build()).await()
                if (isPublic) {
                    s3Client.makeBucketPublic(targetBucket)
                }
                AppResult.Success(Unit)
            } catch (retryException: CancellationException) {
                throw retryException
            } catch (retryException: Exception) {
                AppResult.Error(CommonError.Internal(retryException))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(CommonError.Internal(e))
        }
    }

    private fun resolveTargetBucket(bucket: String?, isPublic: Boolean): String =
        bucket ?: if (isPublic) {
            config.s3BucketNamePublic
        } else {
            config.s3BucketNamePrivate
        }

    private suspend fun S3AsyncClient.makeBucketPublic(bucketName: String) {
        val request = PutBucketPolicyRequest.builder()
            .bucket(bucketName)
            .policy(buildPublicReadPolicy(bucketName))
            .build()

        putBucketPolicy(request).await()
    }

    private fun buildPublicReadPolicy(bucketName: String): String =
        """
        {
            "Version": "2012-10-17",
            "Statement": [
                {
                    "Effect": "Allow",
                    "Principal": "*",
                    "Action": [
                        "s3:GetObject"
                    ],
                    "Resource": [
                        "arn:aws:s3:::$bucketName/*"
                    ]
                }
            ]
        }
        """.trimIndent()

    companion object {
        private const val PRIVATE_FILE_PRESIGNED_URL_EXPIRATION_SECONDS = 86400
        private const val IMMUTABLE_CACHE_CONTROL_HEADER = "public, max-age=31536000, immutable"
    }
}
