package io.github.mudrichenkoevgeny.backend.core.audit.di.module

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepositoryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/**
 * Wires audit persistence components.
 *
 * Binds [AuditEventRepository] to [AuditEventRepositoryImpl].
 */
@Module
interface AuditRepositoriesModule {

    @Binds
    @Singleton
    fun bindAuditEventRepository(auditEventRepositoryImpl: AuditEventRepositoryImpl): AuditEventRepository
}