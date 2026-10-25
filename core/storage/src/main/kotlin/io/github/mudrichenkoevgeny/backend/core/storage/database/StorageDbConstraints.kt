package io.github.mudrichenkoevgeny.backend.core.storage.database

/**
 * Maximum lengths for database string columns specific to the storage module tables.
 * Use these constants when defining Exposed table columns in storage tables.
 */
object StorageDbConstraints {
    /** Maximum length for S3 object keys and paths. */
    const val S3_KEY_MAX_LENGTH = 1024
}