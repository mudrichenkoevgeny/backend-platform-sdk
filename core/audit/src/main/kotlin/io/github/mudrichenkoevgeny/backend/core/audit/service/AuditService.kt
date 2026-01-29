package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.backend.core.audit.model.AuditEvent

interface AuditService {
    fun log(auditEvent: AuditEvent)

    suspend fun awaitAll()
}