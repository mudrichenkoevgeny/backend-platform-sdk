package io.github.mudrichenkoevgeny.backend.core.settings.model

/**
 * Declares how a [SystemSetting.value] should be interpreted by consumers.
 *
 * Values are stored as a string in the database; this enum is used by higher-level APIs to decide
 * how to parse the value when reading it.
 */
enum class SettingType {
    /** A raw string value (no parsing). */
    STRING,
    /** A `Long` value encoded as a string. */
    LONG,
    /** A `Int` value encoded as a string. */
    INT,
    /** A `Double` value encoded as a string. */
    DOUBLE,
    /** A strict boolean (`true`/`false`) encoded as a string. */
    BOOLEAN,
    /** A JSON string that requires a caller-provided deserializer. */
    JSON
}