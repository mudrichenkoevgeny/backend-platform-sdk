package io.github.mudrichenkoevgeny.backend.core.storage.imgproxy

import io.github.mudrichenkoevgeny.shared.foundation.core.storage.domain.model.category.StorageCategory

/**
 * Interface for building signed on-the-fly imgproxy preset URLs.
 */
interface ImgproxyUrlBuilder {

    /**
     * Builds a map of preset keys (such as "thumbnail" or "preview") to signed imgproxy URLs.
     *
     * @param bucket S3 bucket name containing the source object.
     * @param key S3 object key.
     * @param mimeType Source file MIME type (such as `image/jpeg`, `image/svg+xml`, `video/mp4`).
     * @param category Storage category of the file ([StorageCategory]).
     * @return Map of preset key strings to signed imgproxy URLs, or an empty map if disabled or unsupported.
     */
    fun buildPresetUrls(
        bucket: String,
        key: String,
        mimeType: String,
        category: StorageCategory
    ): Map<String, String>
}
