package io.github.mudrichenkoevgeny.backend.core.common.di.module

import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReader
import io.github.mudrichenkoevgeny.backend.core.common.config.env.EnvReaderImpl
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolver
import dagger.Module
import dagger.Provides
import io.github.cdimascio.dotenv.Dotenv
import javax.inject.Singleton

@Module
class EnvModule {

    @Provides
    @Singleton
    fun provideEnvReader(
        pathResolver: PathResolver
    ): EnvReader {
        val paths = pathResolver.getResolvedPaths()

        val dotenv = Dotenv.configure()
            .directory(paths.envFile.parent)
            .filename(paths.envFile.name)
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load()

        return EnvReaderImpl(
            dotenv = dotenv,
            secretsRoot = paths.secretsDir
        )
    }
}