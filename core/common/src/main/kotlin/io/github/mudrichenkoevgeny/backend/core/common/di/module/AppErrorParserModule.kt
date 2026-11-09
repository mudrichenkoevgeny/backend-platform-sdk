package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParserResolver
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.CommonAppErrorSpecificParser
import javax.inject.Singleton

/**
 * Provides the application error parser.
 *
 * Binds [AppErrorParser] implementation to [AppErrorParserResolver].
 */
@Module
public interface AppErrorParserModule {
    @Binds
    @Singleton
    public fun bindAppErrorParser(resolver: AppErrorParserResolver): AppErrorParser

    @Multibinds
    public fun multibindSpecificParsers(): Set<AppErrorSpecificParser>

    @Binds
    @Singleton
    @IntoSet
    public fun bindCommonAppErrorSpecificParser(parser: CommonAppErrorSpecificParser): AppErrorSpecificParser
}
