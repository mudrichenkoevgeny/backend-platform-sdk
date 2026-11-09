package io.github.mudrichenkoevgeny.backend.core.common.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReaderImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolver
import javax.inject.Singleton

/**
 * Provides access to environment configuration files through [EnvReader].
 *
 * Uses resolved paths from [PathResolver] and exposes a singleton [EnvReader] instance
 * that can be used by other configuration factories.
 */
@Module
public class EnvModule {
    @Provides
    @Singleton
    public fun provideEnvReader(pathResolver: PathResolver): EnvReader = EnvReaderImpl(
        paths = pathResolver.getResolvedPaths()
    )
}
