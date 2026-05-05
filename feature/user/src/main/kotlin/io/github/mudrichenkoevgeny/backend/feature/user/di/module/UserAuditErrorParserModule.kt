package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorParser
import io.github.mudrichenkoevgeny.backend.feature.user.audit.error.UserAuditErrorParser

/**
 * Dagger bindings for user-domain audit error parsing.
 *
 * Binds [UserAuditErrorParser] into a set of [AuditErrorParser].
 */
@Module
interface UserAuditErrorParserModule {

    @Binds
    @IntoSet
    fun bindUserAuditErrorParser(parser: UserAuditErrorParser): AuditErrorParser
}