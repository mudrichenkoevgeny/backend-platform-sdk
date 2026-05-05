package io.github.mudrichenkoevgeny.backend.feature.audit.api.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.audit.api.di.module.AuditApiManagersModule

/**
 * Dagger aggregate for the audit HTTP/API feature (`feature/audit-api`).
 */
@Module(includes = [AuditApiManagersModule::class])
interface AuditApiModules
