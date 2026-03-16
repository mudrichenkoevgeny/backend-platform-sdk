package io.github.mudrichenkoevgeny.backend.core.audit.model

import kotlin.uuid.Uuid

/**
 * Stable, unique identifier for a single audit event.
 *
 * Wraps a [Uuid] to provide type safety where audit event identifiers
 * are used (e.g. repository, API responses).
 *
 * @param value UUID backing the id; use [asHexDashString] for serialization.
 */
@JvmInline
value class AuditEventId(val value: Uuid) {

    /**
     * Returns the id as a canonical hex string with dashes (e.g. for API or logging).
     */
    fun asHexDashString(): String = value.toHexDashString()

    companion object {

        /**
         * Generates a new random [AuditEventId].
         */
        fun generate() = AuditEventId(Uuid.random())
    }
}

/**
 * Attempts to parse this string as an [AuditEventId].
 *
 * Returns `null` if the value is not a valid UUID in hex-with-dashes form.
 */
fun String.toAuditEventIdOrNull(): AuditEventId? =
    Uuid.parseOrNull(this)?.let { AuditEventId(it) }

/**
 * Parses this string into an [AuditEventId] or throws if the value is not a valid UUID.
 */
fun String.toAuditEventIdOrThrow(): AuditEventId = AuditEventId(Uuid.parse(this))