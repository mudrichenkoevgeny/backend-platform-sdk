package io.github.mudrichenkoevgeny.backend.core.common.config.model

enum class AppEnvironment {
    DEV, TEST, PROD;

    companion object {
        fun fromString(value: String?): AppEnvironment {
            return try {
                value?.uppercase()?.let { valueOf(it) } ?: DEV
            } catch (_: IllegalArgumentException) {
                DEV
            }
        }
    }
}