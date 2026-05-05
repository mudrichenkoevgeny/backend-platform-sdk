package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepositoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepositoryImpl
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings.UserTotpSettingsRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usertotpsettings.UserTotpSettingsRepositoryImpl
import javax.inject.Singleton

/**
 * Binds database repositories used by the user feature.
 *
 * Maps repository interfaces to their default Exposed-based implementations.
 */
@Module
interface UserRepositoriesModule {

    @Binds
    @Singleton
    fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    fun bindUserIdentifierRepository(
        userIdentifierRepositoryImpl: UserIdentifierRepositoryImpl
    ): UserIdentifierRepository

    @Binds
    @Singleton
    fun bindUserSessionRepository(
        userSessionRepositoryImpl: UserSessionRepositoryImpl
    ): UserSessionRepository

    @Binds
    @Singleton
    fun bindUserTotpSettingsRepository(
        userTotpSettingsRepositoryImpl: UserTotpSettingsRepositoryImpl
    ): UserTotpSettingsRepository
}