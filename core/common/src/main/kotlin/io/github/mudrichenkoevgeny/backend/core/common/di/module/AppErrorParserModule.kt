package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParserImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Provides the application error parser and its configuration.
 *
 * Binds [AppErrorParser] implementation and exposes [AppErrorParserConfig]
 * obtained from [AppErrorParserConfigHolder].
 */
@Module
interface AppErrorParserModule {

    @Binds
    @Singleton
    fun bindAppErrorParser(appErrorParserImpl: AppErrorParserImpl): AppErrorParser

    companion object {
        @Provides
        @Singleton
        fun provideAppErrorParserConfig(): AppErrorParserConfig {
            return AppErrorParserConfigHolder.get()
        }
    }
}