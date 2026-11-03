package io.github.mudrichenkoevgeny.backend.core.storage.imgproxy

/**
 * Interface for building signed on-the-fly imgproxy preset URLs.
 */
interface ImgproxyUrlBuilder {

    /**
     * Builds a map of preset keys (such as "thumbnail" or "avatar_sm") to signed imgproxy URLs.
     *
     * @param bucket S3 bucket name containing the source object.
     * @param key S3 object key.
     * @param mimeType Source file MIME type (such as `image/jpeg`, `image/svg+xml`, `video/mp4`).
     * @return Map of preset key strings to signed imgproxy URLs, or an empty map if disabled or unsupported.
     */
    fun buildPresetUrls(bucket: String, key: String, mimeType: String): Map<String, String>
}
