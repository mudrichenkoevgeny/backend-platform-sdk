package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepositoryImpl
import javax.inject.Singleton

/**
 * Wires audit persistence components.
 *
 * Binds [AuditEventRepository] to [AuditEventRepositoryImpl].
 */
@Module
public interface AuditRepositoriesModule {
    @Binds
    @Singleton
    public fun bindAuditEventRepository(auditEventRepositoryImpl: AuditEventRepositoryImpl): AuditEventRepository
}
