package io.github.mudrichenkoevgeny.backend.core.storage.model

import io.github.mudrichenkoevgeny.backend.core.storage.service.StorageService

/**
 * Storage backend type used by the storage module.
 *
 * Determines which [StorageService] implementation is selected by DI.
 */
enum class StorageType {
    S3, LOCAL;

    companion object {
        /**
         * Parses a string into [StorageType].
         *
         * @param value configuration value (e.g. "S3", "LOCAL"); may be `null` or blank.
         * @return the corresponding [StorageType]; returns [LOCAL] when value is `null`, blank, or unrecognized.
         */
        fun fromString(value: String?): StorageType {
            return try {
                value?.uppercase()?.let { StorageType.valueOf(it) } ?: LOCAL
            } catch (_: IllegalArgumentException) {
                LOCAL
            }
        }
    }
}