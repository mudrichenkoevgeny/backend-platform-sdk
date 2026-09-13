package io.github.mudrichenkoevgeny.backend.core.security.di.module

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.security.error.parser.SecurityErrorParser
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Binds [SecurityErrorParser] into the Set of [AppErrorSpecificParser]s.
 */
@Module
interface SecurityAppErrorParserModule {
    @Binds
    @Singleton
    @IntoSet
    fun bindSecurityErrorParser(parser: SecurityErrorParser): AppErrorSpecificParser
}
