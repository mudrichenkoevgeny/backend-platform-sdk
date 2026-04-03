package io.github.mudrichenkoevgeny.backend.core.audit.di

import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditManagersModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditServicesModule
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditLoggerModule

/**
 * Aggregates core audit modules that are typically used by applications that need audit logging.
 *
 * This module only wires repository, manager, and service bindings; it expects database
 * and configuration to be provided by the application component.
 */
@Module(
    includes = [
        AuditRepositoriesModule::class,
        AuditManagersModule::class,
        AuditServicesModule::class,
        AuditLoggerModule::class
    ]
)
interface AuditModules