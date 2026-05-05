package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.scheduled.UserScheduledJobs
import io.github.mudrichenkoevgeny.backend.feature.user.scheduled.UserScheduledJobsImpl
import javax.inject.Singleton

/**
 * Dagger module for binding scheduled job implementations within the user feature.
 *
 * Configures [UserScheduledJobs] as a singleton by binding its concrete implementation [UserScheduledJobsImpl].
 */
@Module
interface UserScheduledJobsModule {

    @Binds
    @Singleton
    fun bindUserScheduledJobs(impl: UserScheduledJobsImpl): UserScheduledJobs
}
