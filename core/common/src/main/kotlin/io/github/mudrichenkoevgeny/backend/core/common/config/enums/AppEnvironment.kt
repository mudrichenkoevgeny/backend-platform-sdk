package io.github.mudrichenkoevgeny.backend.core.common.config.enums

enum class AppEnvironment {
    DEV, TEST, PROD;

    companion object {
        fun fromString(value: String?): AppEnvironment {
            return try {
                value?.uppercase()?.let { AppEnvironment.valueOf(it) } ?: DEV
            } catch (_: IllegalArgumentException) {
                DEV
            }
        }
    }
}