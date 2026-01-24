package com.mudrichenkoevgeny.backend.feature.user.di.module

import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import com.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLoggerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface UserAuditModule {

    @Binds
    @Singleton
    fun bindUserAuditLogger(userAuditLoggerImpl: UserAuditLoggerImpl): UserAuditLogger
}