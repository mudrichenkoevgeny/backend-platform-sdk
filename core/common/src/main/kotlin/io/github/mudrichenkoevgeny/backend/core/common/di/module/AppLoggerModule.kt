package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BusinessLogger
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.SystemLogger
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLoggerImpl
import dagger.Module
import dagger.Provides
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Module
class AppLoggerModule {

    @Provides
    @Singleton
    @SystemLogger
    fun provideSystemLogger(): Logger = LoggerFactory.getLogger(AppLogger.SYSTEM_LOGGER)

    @Provides
    @Singleton
    @BusinessLogger
    fun provideBusinessLogger(): Logger = LoggerFactory.getLogger(AppLogger.BUSINESS_LOGGER)

    @Provides
    @Singleton
    fun provideAppLogger(
        @SystemLogger systemLogger: Logger,
        @BusinessLogger businessLogger: Logger
    ): AppLogger = AppLoggerImpl(
        systemLogger = systemLogger,
        businessLogger = businessLogger
    )
}