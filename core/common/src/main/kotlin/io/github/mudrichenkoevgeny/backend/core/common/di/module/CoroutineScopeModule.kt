package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
class CoroutineScopeModule {

    @Provides
    @Singleton
    @BackgroundScope
    fun provideBackgroundScope(): CoroutineScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())
}