package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * Provides default application-wide [Clock] binding.
 *
 * Uses [Clock.System] as default implementation. Applications or tests
 * may override this binding if custom time behavior is required.
 */
@Module
public object ClockModule {

    @Provides
    @Singleton
    public fun provideClock(): Clock = Clock.System
}
