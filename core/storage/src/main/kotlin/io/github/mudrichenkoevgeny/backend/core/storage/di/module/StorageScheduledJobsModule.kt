package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.core.storage.scheduled.StorageScheduledJobs
import io.github.mudrichenkoevgeny.backend.core.storage.scheduled.StorageScheduledJobsImpl
import javax.inject.Singleton

/**
 * Dagger module for binding scheduled job implementations within the storage module.
 *
 * Configures [StorageScheduledJobs] as a singleton by binding its concrete implementation [StorageScheduledJobsImpl].
 */
@Module
interface StorageScheduledJobsModule {

    @Binds
    @Singleton
    fun bindStorageScheduledJobs(impl: StorageScheduledJobsImpl): StorageScheduledJobs
}
