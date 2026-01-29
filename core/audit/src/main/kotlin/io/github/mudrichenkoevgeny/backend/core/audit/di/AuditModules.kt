package io.github.mudrichenkoevgeny.backend.core.audit.di

import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditManagersModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditRepositoriesModule
import io.github.mudrichenkoevgeny.backend.core.audit.di.module.AuditServicesModule
import dagger.Module

@Module(
    includes = [
        AuditRepositoriesModule::class,
        AuditManagersModule::class,
        AuditServicesModule::class
    ]
)
interface AuditModules