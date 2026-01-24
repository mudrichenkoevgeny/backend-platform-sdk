package com.mudrichenkoevgeny.backend.core.audit.service

import com.mudrichenkoevgeny.backend.core.audit.model.AuditEvent

interface AuditService {
    fun log(auditEvent: AuditEvent)

    suspend fun awaitAll()
}