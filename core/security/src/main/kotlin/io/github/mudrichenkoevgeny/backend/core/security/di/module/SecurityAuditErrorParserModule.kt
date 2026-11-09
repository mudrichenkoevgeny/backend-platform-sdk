package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorParser
import io.github.mudrichenkoevgeny.backend.core.security.audit.error.SecurityAuditErrorParser

/**
 * Dagger bindings for security-related audit error parsing.
 *
 * Binds [SecurityAuditErrorParser] into a set of [AuditErrorParser].
 */
@Module
public interface SecurityAuditErrorParserModule {
    @Binds
    @IntoSet
    public fun bindSecurityAuditErrorParser(parser: SecurityAuditErrorParser): AuditErrorParser
}
