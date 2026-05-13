package io.github.mudrichenkoevgeny.backend.feature.auditapi.di

import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.auditapi.api.di.module.AuditApiManagersModule

/**
 * Dagger aggregate for the audit HTTP/API feature (`feature/auditapi`).
 */
@Module(includes = [AuditApiManagersModule::class])
interface AuditApiModules
