package io.github.mudrichenkoevgeny.backend.core.events.model

/**
 * Event transport type used by the events module.
 *
 * Selects which publisher/subscriber implementation should be used by the host application.
 */
enum class EventsType {

    /** Apache Kafka-based transport. */
    KAFKA,

    /** In-memory transport (useful for local development or tests). */
    IN_MEMORY;

    companion object {

        /**
         * Parses a string into [EventsType].
         *
         * @param value environment variable value (e.g. "KAFKA", "IN_MEMORY"); may be `null` or blank.
         * @return the corresponding [EventsType]; returns [KAFKA] when value is `null`, blank, or unrecognized.
         */
        fun fromString(value: String?): EventsType {
            return try {
                value?.uppercase()?.let { valueOf(it) } ?: KAFKA
            } catch (_: IllegalArgumentException) {
                KAFKA
            }
        }
    }
}
