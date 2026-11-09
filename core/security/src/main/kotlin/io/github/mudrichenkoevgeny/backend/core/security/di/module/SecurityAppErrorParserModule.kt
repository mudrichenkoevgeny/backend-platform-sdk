package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.security.error.parser.SecurityErrorParser
import javax.inject.Singleton

/**
 * Binds [SecurityErrorParser] into the Set of [AppErrorSpecificParser]s.
 */
@Module
public interface SecurityAppErrorParserModule {
    @Binds
    @Singleton
    @IntoSet
    public fun bindSecurityErrorParser(parser: SecurityErrorParser): AppErrorSpecificParser
}
