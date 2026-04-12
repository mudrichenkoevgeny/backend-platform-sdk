package io.github.mudrichenkoevgeny.backend.feature.audit.api.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.audit.api.di.module.AuditApiManagersModule
import io.github.mudrichenkoevgeny.backend.feature.audit.api.manager.AuditManager
import io.github.mudrichenkoevgeny.backend.feature.audit.api.route.AuditRouter

/**
 * Dagger aggregate for the audit HTTP/API feature (`feature/audit-api`).
 *
 * Include this in the application component when wiring [AuditRouter] or any type that injects [AuditManager].
 * Core audit persistence remains in `AuditModules` from `core/audit`.
 */
@Module(includes = [AuditApiManagersModule::class])
interface AuditApiModules
