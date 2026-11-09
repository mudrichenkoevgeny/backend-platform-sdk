package io.github.mudrichenkoevgeny.backend.core.storage.config.model

/**
 * Configuration parameters for the imgproxy media preview and thumbnail generation service.
 *
 * Holds connection settings, HMAC signing keys, preset specifications, and default video frame extraction settings.
 *
 * @param isEnabled whether imgproxy preview and thumbnail generation is enabled.
 * @param baseUrl base URL of the imgproxy service.
 * @param key hex-encoded secret key used for HMAC-SHA256 URL signing.
 * @param salt hex-encoded salt used for HMAC-SHA256 URL signing.
 * @param presets map of preset keys (such as "thumbnail" or "avatar_sm") to [ImgproxyPreset] specifications.
 * @param videoThumbnailSecond default seek time in seconds when extracting thumbnail frames from video files.
 */
public data class ImgproxyConfig(
    val isEnabled: Boolean,
    val baseUrl: String,
    val key: String,
    val salt: String,
    val presets: Map<String, ImgproxyPreset> = emptyMap(),
    val videoThumbnailSecond: Int
)
