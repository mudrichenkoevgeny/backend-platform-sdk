package io.github.mudrichenkoevgeny.backend.core.audit.di

import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditLoggerModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditServicesModule
import dagger.Module

/**
 * Aggregates core audit modules for repository, service, and logging.
 *
 * Expects database and common configurations to be provided by the application.
 */
@Module(
    includes = [
        AuditRepositoriesModule::class,
        AuditServicesModule::class,
        AuditLoggerModule::class
    ]
)
interface AuditModules