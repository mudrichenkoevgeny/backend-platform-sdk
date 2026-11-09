package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal

/**
 * Fire-and-forget audit logging API.
 *
 * [log] enqueues an event to be persisted asynchronously and returns immediately.
 * Use [awaitAll] in tests or shutdown hooks to wait until all enqueued events are written.
 */
public interface AuditService {
    /**
     * Schedules [auditEvent] to be persisted in the background. Returns immediately.
     */
    public fun log(auditEvent: AuditEventInternal)

    /**
     * Suspends until all events previously passed to [log] have been processed.
     * Primarily for tests or graceful shutdown.
     */
    public suspend fun awaitAll()
}
