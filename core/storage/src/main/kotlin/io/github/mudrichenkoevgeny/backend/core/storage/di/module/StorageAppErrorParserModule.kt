package io.github.mudrichenkoevgeny.backend.core.storage.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.storage.error.parser.StorageErrorParser
import javax.inject.Singleton

/**
 * Binds [StorageErrorParser] into the Set of [AppErrorSpecificParser]s.
 */
@Module
interface StorageAppErrorParserModule {

    @Binds
    @Singleton
    @IntoSet
    fun bindStorageErrorParser(parser: StorageErrorParser): AppErrorSpecificParser
}