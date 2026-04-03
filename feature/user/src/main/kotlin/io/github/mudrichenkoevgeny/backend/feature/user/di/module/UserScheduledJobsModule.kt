package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserScheduledJobsConfig
import io.github.mudrichenkoevgeny.backend.feature.user.scheduled.UserScheduledJobs
import io.github.mudrichenkoevgeny.backend.feature.user.scheduled.UserScheduledJobsImpl
import javax.inject.Singleton

@Module
/**
 * Binds [UserScheduledJobs] and exposes [UserScheduledJobsConfig] from [UserConfig].
 */
interface UserScheduledJobsModule {

    @Binds
    @Singleton
    fun bindUserScheduledJobs(impl: UserScheduledJobsImpl): UserScheduledJobs

    companion object {

        @Provides
        @Singleton
        fun provideUserScheduledJobsConfig(userConfig: UserConfig): UserScheduledJobsConfig =
            userConfig.scheduledJobs
    }
}
