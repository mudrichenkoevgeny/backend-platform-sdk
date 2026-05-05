package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLoggerImpl
import javax.inject.Singleton

/**
 * Wires audit logging components.
 *
 * Binds [AuditLogger] to [AuditLoggerImpl].
 */
@Module
interface AuditLoggerModule {

    @Binds
    @Singleton
    fun bindAuditLogger(auditLoggerImpl: AuditLoggerImpl): AuditLogger
}