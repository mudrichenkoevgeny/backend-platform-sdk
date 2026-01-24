package com.mudrichenkoevgeny.backend.core.storage.service

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class S3StorageService @Inject constructor(
    private val config: StorageConfig
) : StorageService {

    private val s3Client = S3AsyncClient.builder()
        .endpointOverride(URI.create(config.s3Endpoint))
        .region(Region.of(config.s3Region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(config.s3AccessKey, config.s3SecretKey)
            )
        )
        .forcePathStyle(config.forcePathStyle)
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
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
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
        } catch (t: Throwable) {
            AppResult.Error(CommonError.System(t))
        }
    }

    override fun getUrl(key: String): AppResult<String> {
        val baseUrl = config.s3PublicUrl.removeSuffix("/")
        return AppResult.Success("$baseUrl/$key")
    }
}