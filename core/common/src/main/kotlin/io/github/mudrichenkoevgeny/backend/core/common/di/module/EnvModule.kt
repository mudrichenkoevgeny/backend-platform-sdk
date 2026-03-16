package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReaderImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolver
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Provides access to environment configuration files through [EnvReader].
 *
 * Uses resolved paths from [PathResolver] and exposes a singleton [EnvReader] instance
 * that can be used by other configuration factories.
 */
@Module
class EnvModule {

    @Provides
    @Singleton
    fun provideEnvReader(
        pathResolver: PathResolver
    ): EnvReader {
        return EnvReaderImpl(
            paths = pathResolver.getResolvedPaths()
        )
    }
}