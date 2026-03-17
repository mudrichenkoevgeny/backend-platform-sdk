package io.github.mudrichenkoevgeny.backend.core.events.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventsTypeTest {

    @Test
    fun `fromString returns KAFKA when value is null`() {
        assertEquals(EventsType.KAFKA, EventsType.fromString(null))
    }

    @Test
    fun `fromString returns KAFKA when value is blank`() {
        assertEquals(EventsType.KAFKA, EventsType.fromString("   "))
    }

    @Test
    fun `fromString returns KAFKA when value is unrecognized`() {
        assertEquals(EventsType.KAFKA, EventsType.fromString("rabbitmq"))
    }

    @Test
    fun `fromString is case-insensitive`() {
        assertEquals(EventsType.IN_MEMORY, EventsType.fromString("in_memory"))
        assertEquals(EventsType.KAFKA, EventsType.fromString("kAfKa"))
    }
}

