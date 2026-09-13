package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.feature.user.error.parser.UserErrorParser
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Binds [UserErrorParser] into the Set of [AppErrorSpecificParser]s.
 */
@Module
interface UserAppErrorParserModule {
    @Binds
    @Singleton
    @IntoSet
    fun bindUserErrorParser(parser: UserErrorParser): AppErrorSpecificParser
}
