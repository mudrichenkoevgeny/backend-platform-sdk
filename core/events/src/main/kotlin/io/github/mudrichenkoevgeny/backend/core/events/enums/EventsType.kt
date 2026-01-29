package io.github.mudrichenkoevgeny.backend.core.events.enums

enum class EventsType {
    KAFKA, IN_MEMORY;

    companion object {
        fun fromString(value: String?): EventsType {
            return try {
                value?.uppercase()?.let { EventsType.valueOf(it) } ?: KAFKA
            } catch (_: IllegalArgumentException) {
                KAFKA
            }
        }
    }
}