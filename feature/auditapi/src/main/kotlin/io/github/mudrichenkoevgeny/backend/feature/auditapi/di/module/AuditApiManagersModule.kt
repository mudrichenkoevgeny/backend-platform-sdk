package io.github.mudrichenkoevgeny.backend.feature.auditapi.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.auditapi.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.auditapi.manager.AuditManagerImpl
import javax.inject.Singleton

/**
 * Binds feature-level [AuditManager] for HTTP use cases and any other injectable readers.
 */
@Module
interface AuditApiManagersModule {

    @Binds
    @Singleton
    fun bindAuditManager(impl: AuditManagerImpl): AuditManager
}
