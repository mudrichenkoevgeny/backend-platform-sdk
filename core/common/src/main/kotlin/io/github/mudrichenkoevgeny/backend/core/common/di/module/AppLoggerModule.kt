package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BusinessLogger
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.SystemLogger
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLoggerImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
 * Provides logging infrastructure used across the common layer.
 *
 * Exposes:
 * - qualified [Logger] instances for system and business logging;
 * - high-level [AppLogger] that wraps both.
 *
 * Applications may override these bindings if they need custom logging backends.
 */
@Module
public class AppLoggerModule {
    @Provides
    @Singleton
    @SystemLogger
    public fun provideSystemLogger(): Logger = LoggerFactory.getLogger(AppLogger.SYSTEM_LOGGER)

    @Provides
    @Singleton
    @BusinessLogger
    public fun provideBusinessLogger(): Logger = LoggerFactory.getLogger(AppLogger.BUSINESS_LOGGER)

    @Provides
    @Singleton
    public fun provideAppLogger(
        @SystemLogger systemLogger: Logger,
        @BusinessLogger businessLogger: Logger
    ): AppLogger = AppLoggerImpl(
        systemLogger = systemLogger,
        businessLogger = businessLogger
    )
}
