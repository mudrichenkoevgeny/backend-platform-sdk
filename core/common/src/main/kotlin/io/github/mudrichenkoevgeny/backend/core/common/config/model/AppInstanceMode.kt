package io.github.mudrichenkoevgeny.backend.core.common.config.model

/**
 * Functional role of the application instance.
 *
 * Defines which API routes and background services (migrations, seeders, jobs)
 * are initialized at runtime.
 */
public enum class AppInstanceMode {
    /** Public-facing API for end-users. Minimal background processing. */
    PUBLIC,

    /** Administrative and management API. Runs migrations and scheduled tasks. */
    MANAGEMENT,

    /** Hybrid mode combining all roles. */
    FULL;

    public companion object {
        /**
         * Parses a nullable string into [AppInstanceMode], defaulting to [FULL] on null or invalid input.
         *
         * @param value raw instance mode value (case-insensitive).
         */
        public fun fromString(value: String?): AppInstanceMode = try {
            value?.uppercase()?.let { valueOf(it) } ?: FULL
        } catch (_: IllegalArgumentException) {
            FULL
        }
    }
}
