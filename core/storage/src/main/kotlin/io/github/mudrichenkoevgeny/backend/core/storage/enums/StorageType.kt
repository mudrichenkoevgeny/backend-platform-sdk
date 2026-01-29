package io.github.mudrichenkoevgeny.backend.core.storage.enums

enum class StorageType {
    S3, LOCAL;

    companion object {
        fun fromString(value: String?): StorageType {
            return try {
                value?.uppercase()?.let { StorageType.valueOf(it) } ?: LOCAL
            } catch (_: IllegalArgumentException) {
                LOCAL
            }
        }
    }
}