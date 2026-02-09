package io.github.mudrichenkoevgeny.backend.core.events.model

enum class EventsType {
    KAFKA, IN_MEMORY;

    companion object {
        fun fromString(value: String?): EventsType {
            return try {
                value?.uppercase()?.let { valueOf(it) } ?: KAFKA
            } catch (_: IllegalArgumentException) {
                KAFKA
            }
        }
    }
}