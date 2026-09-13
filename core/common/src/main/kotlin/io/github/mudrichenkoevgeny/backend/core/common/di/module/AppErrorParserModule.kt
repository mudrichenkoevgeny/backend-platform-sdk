package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParserResolver
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.CommonAppErrorSpecificParser
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import javax.inject.Singleton

/**
 * Provides the application error parser and its configuration.
 *
 * Binds [AppErrorParser] implementation to [AppErrorParserResolver]
 * and exposes [AppErrorParserConfig] obtained from [AppErrorParserConfigHolder].
 */
@Module
interface AppErrorParserModule {

    @Binds
    @Singleton
    fun bindAppErrorParser(resolver: AppErrorParserResolver): AppErrorParser

    @Multibinds
    fun multibindSpecificParsers(): Set<AppErrorSpecificParser>

    @Binds
    @Singleton
    @IntoSet
    fun bindCommonAppErrorSpecificParser(parser: CommonAppErrorSpecificParser): AppErrorSpecificParser

    companion object {
        @Provides
        @Singleton
        fun provideAppErrorParserConfig(): AppErrorParserConfig {
            return AppErrorParserConfigHolder.get()
        }
    }
}