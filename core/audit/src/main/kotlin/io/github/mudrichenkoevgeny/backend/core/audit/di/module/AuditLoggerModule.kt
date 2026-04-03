package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLoggerImpl
import javax.inject.Singleton

@Module
/**
 * Binds audit logging abstractions.
 */
interface AuditLoggerModule {

    @Binds
    @Singleton
    fun bindAuditLogger(auditLoggerImpl: AuditLoggerImpl): AuditLogger
}