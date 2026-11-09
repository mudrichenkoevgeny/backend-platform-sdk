package io.github.mudrichenkoevgeny.backend.core.storage.database

/**
 * Maximum lengths for database string columns specific to the storage module tables.
 * Use these constants when defining Exposed table columns in storage tables.
 */
public object StorageDbConstraints {
    /** Maximum length for S3 object keys and paths. */
    public const val S3_KEY_MAX_LENGTH: Int = 1024
}
