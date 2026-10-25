package io.github.mudrichenkoevgeny.backend.core.storage.domain.model

/**
 * Metadata retrieved from an S3 object.
 *
 * @property sizeBytes Size of the object in bytes.
 * @property eTag Entity tag (ETag) representing the object content hash, typically enclosed quotes stripped.
 */
data class S3ObjectMetadata(
    val sizeBytes: Long,
    val eTag: String?
)