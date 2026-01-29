package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParserImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface AppErrorParserModule {

    @Binds
    @Singleton
    fun bindAppErrorParser(appErrorParserImpl: AppErrorParserImpl): AppErrorParser
}