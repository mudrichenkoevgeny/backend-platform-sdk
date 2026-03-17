package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLogger
import io.github.mudrichenkoevgeny.backend.feature.user.audit.logger.UserAuditLoggerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
/**
 * Binds audit logging abstractions for the user feature.
 *
 * Maps [UserAuditLogger] to [UserAuditLoggerImpl].
 */
interface UserAuditModule {

    @Binds
    @Singleton
    fun bindUserAuditLogger(userAuditLoggerImpl: UserAuditLoggerImpl): UserAuditLogger
}