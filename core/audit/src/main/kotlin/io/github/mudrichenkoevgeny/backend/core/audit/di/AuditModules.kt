package io.github.mudrichenkoevgeny.backend.core.audit.di

import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditLoggerModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditServicesModule
import dagger.Module

/**
 * Aggregates core audit modules that are typically used by applications that need audit logging.
 *
 * Wires `AuditEventRepository` and `AuditService`; it expects database and configuration
 * to be provided by the application component.
 *
 * HTTP/query-facing `AuditManager` from the `feature/audit-api` module is bound via `AuditApiModules`.
 */
@Module(
    includes = [
        AuditRepositoriesModule::class,
        AuditServicesModule::class,
        AuditLoggerModule::class
    ]
)
interface AuditModules