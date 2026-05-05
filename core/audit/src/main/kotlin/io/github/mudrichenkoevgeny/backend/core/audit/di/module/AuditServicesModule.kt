package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditServiceImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/**
 * Wires audit business logic services.
 *
 * Binds [AuditService] to [AuditServiceImpl].
 */
@Module
interface AuditServicesModule {

    @Binds
    @Singleton
    fun bindAuditService(auditServiceImpl: AuditServiceImpl): AuditService
}