package io.github.mudrichenkoevgeny.backend.core.database

/**
 * Default maximum lengths for database string columns used across SDK tables.
 * Use these constants when defining Exposed table columns to keep schema consistent.
 */
public object BaseDbConstraints {
    public const val DEFAULT_MAX_LENGTH: Int = 255
    public const val ENUM_MAX_LENGTH: Int = 32
    public const val IP_MAX_LENGTH: Int = 64
    public const val LANGUAGE_MAX_LENGTH: Int = 255
    public const val VERSION_MAX_LENGTH: Int = 64
}
