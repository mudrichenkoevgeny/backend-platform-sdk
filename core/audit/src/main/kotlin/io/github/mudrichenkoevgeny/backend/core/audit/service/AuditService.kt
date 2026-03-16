package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent

/**
 * Fire-and-forget audit logging API.
 *
 * [log] enqueues an event to be persisted asynchronously and returns immediately.
 * Use [awaitAll] in tests or shutdown hooks to wait until all enqueued events are written.
 */
interface AuditService {

    /**
     * Schedules [auditEvent] to be persisted in the background. Returns immediately.
     */
    fun log(auditEvent: AuditEvent)

    /**
     * Suspends until all events previously passed to [log] have been processed.
     * Primarily for tests or graceful shutdown.
     */
    suspend fun awaitAll()
}