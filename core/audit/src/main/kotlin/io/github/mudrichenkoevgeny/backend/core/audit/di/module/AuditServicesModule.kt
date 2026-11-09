package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditService
import io.github.mudrichenkoevgeny.backend.core.audit.service.AuditServiceImpl
import javax.inject.Singleton

/**
 * Wires audit business logic services.
 *
 * Binds [AuditService] to [AuditServiceImpl].
 */
@Module
public interface AuditServicesModule {
    @Binds
    @Singleton
    public fun bindAuditService(auditServiceImpl: AuditServiceImpl): AuditService
}
