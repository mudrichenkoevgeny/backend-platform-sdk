package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.feature.user.error.parser.UserErrorParser
import javax.inject.Singleton

/**
 * Binds [UserErrorParser] into the Set of [AppErrorSpecificParser]s.
 */
@Module
public interface UserAppErrorParserModule {
    @Binds
    @Singleton
    @IntoSet
    public fun bindUserErrorParser(parser: UserErrorParser): AppErrorSpecificParser
}
