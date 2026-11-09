package io.github.mudrichenkoevgeny.backend.core.storage.imgproxy

import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyConfig
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.ImgproxyPreset
import io.github.mudrichenkoevgeny.backend.core.storage.config.model.StorageConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory
import java.util.Base64
import java.util.HexFormat
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [ImgproxyUrlBuilder] using HMAC-SHA256 signing and Base64url encoding.
 *
 * Encodes S3 URLs in `s3://<bucket>/<key>` format using Base64url without padding.
 *
 * @param storageConfig Configuration parameters holding [ImgproxyConfig].
 */
@Singleton
public class ImgproxyUrlBuilderImpl @Inject constructor(
    private val storageConfig: StorageConfig
) : ImgproxyUrlBuilder {
    override fun buildPresetUrls(
        bucket: String,
        key: String,
        mimeType: String,
        category: StorageCategory
    ): Map<String, String> {
        val config = storageConfig.imgproxyConfig ?: return emptyMap()
        if (!config.isEnabled || config.presets.isEmpty()) {
            return emptyMap()
        }

        if (!isSupportedMimeType(mimeType)) {
            return emptyMap()
        }

        val result = mutableMapOf<String, String>()
        for ((presetKey, presetSpec) in config.presets) {
            val options = getProcessingOptions(mimeType, presetSpec)
            val signedUrl = buildSignedUrl(config, bucket, key, options)
            result[presetKey] = signedUrl
        }
        return result
    }

    private fun isSupportedMimeType(mimeType: String): Boolean = isRasterImage(mimeType) || isVectorSvg(mimeType)

    private fun getProcessingOptions(
        mimeType: String,
        presetSpec: ImgproxyPreset
    ): String {
        val size = presetSpec.sizePixels
        val resizeType = presetSpec.resizeType.value
        val resizeOption = "rs:$resizeType:$size:$size"

        return when {
            isRasterImage(mimeType) -> resizeOption
            isVectorSvg(mimeType) -> "$resizeOption/$SVG_FORMAT_OPTION"
            else -> resizeOption
        }
    }

    private fun isRasterImage(mimeType: String): Boolean = mimeType in RASTER_IMAGE_MIME_TYPES

    private fun isVectorSvg(mimeType: String): Boolean = mimeType == SVG_MIME_TYPE

    private fun buildSignedUrl(
        config: ImgproxyConfig,
        bucket: String,
        key: String,
        processingOptions: String
    ): String {
        val s3SourceUrl = "$S3_SCHEME_PREFIX$bucket/$key"
        val encodedSourceUrl = Base64.getUrlEncoder().withoutPadding().encodeToString(
            s3SourceUrl.toByteArray(Charsets.UTF_8)
        )
        val path = "/$processingOptions/$encodedSourceUrl"

        val signature = if (config.key.isNotBlank() && config.salt.isNotBlank()) {
            calculateSignature(path, config.key, config.salt)
        } else {
            INSECURE_SIGNATURE
        }

        val baseUrl = config.baseUrl.removeSuffix("/")
        return "$baseUrl/$signature$path"
    }

    private fun calculateSignature(
        path: String,
        keyHex: String,
        saltHex: String
    ): String {
        val keyBytes = HexFormat.of().parseHex(keyHex)
        val saltBytes = HexFormat.of().parseHex(saltHex)
        val pathBytes = path.toByteArray(Charsets.UTF_8)

        val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
        mac.init(SecretKeySpec(keyBytes, HMAC_SHA256_ALGORITHM))
        mac.update(saltBytes)
        mac.update(pathBytes)
        val digest = mac.doFinal()

        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    public companion object {
        private const val SVG_MIME_TYPE = "image/svg+xml"
        private const val SVG_FORMAT_OPTION = "fmt:png"
        private const val S3_SCHEME_PREFIX = "s3://"
        private const val INSECURE_SIGNATURE = "insecure"
        private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"

        private val RASTER_IMAGE_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/heic",
            "image/avif"
        )
    }
}
