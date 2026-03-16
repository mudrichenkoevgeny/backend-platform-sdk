package io.github.mudrichenkoevgeny.backend.core.audit.model

import kotlinx.serialization.json.JsonElement
import kotlin.uuid.Uuid

/**
 * Immutable record of an audited action in the system.
 *
 * Captures who ([actorId]) performed which [action] on which [resource] (and optional [resourceId]),
 * with [status] and optional [message] and [metadata] for diagnostics.
 *
 * @param id Unique event id; defaults to [AuditEventId.generate] when creating new events.
 * @param actorId Optional identifier of the user or system that performed the action.
 * @param action Action name (e.g. "login", "create_order").
 * @param resource Resource type (e.g. "order", "user").
 * @param resourceId Optional id of the specific resource instance.
 * @param status Outcome of the action ([AuditStatus]).
 * @param metadata Optional key-value data for filtering or analytics.
 * @param message Optional human-readable message (e.g. error description).
 */
data class AuditEvent(
    val id: AuditEventId = AuditEventId.generate(),
    val actorId: Uuid? = null,
    val action: String,
    val resource: String,
    val resourceId: String? = null,
    val status: AuditStatus,
    val metadata: Map<String, JsonElement> = emptyMap(),
    val message: String? = null
)