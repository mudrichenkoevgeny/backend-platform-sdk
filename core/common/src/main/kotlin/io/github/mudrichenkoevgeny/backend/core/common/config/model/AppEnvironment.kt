package io.github.mudrichenkoevgeny.backend.core.common.config.model

/**
 * Logical application environment used to adjust behavior (logging, features, etc.).
 */
public enum class AppEnvironment {
    /** Local or developer environment with relaxed constraints. */
    DEV,

    /** Automated tests and CI environments. */
    TEST,

    /** Production environment serving real users. */
    PROD

    ;

    public companion object {
        /**
         * Parses a nullable string into [AppEnvironment], defaulting to [DEV] on null or invalid input.
         *
         * @param value raw environment value (case-insensitive).
         */
        public fun fromString(value: String?): AppEnvironment = try {
            value?.uppercase()?.let { valueOf(it) } ?: DEV
        } catch (_: IllegalArgumentException) {
            DEV
        }
    }
}
