package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.identifier.IdentifierManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.totp.TotpManagerImpl
import javax.inject.Singleton

/**
 * Binds feature managers that orchestrate user/auth/session operations.
 *
 * Managers are used by use cases and routes to coordinate repositories and services.
 */
@Module
interface UserManagersModule {

    @Binds
    @Singleton
    fun bindUserManager(userManagerImpl: UserManagerImpl): UserManager

    @Binds
    @Singleton
    fun bindUserIdentifierManager(userIdentifierManagerImpl: IdentifierManagerImpl): IdentifierManager

    @Binds
    @Singleton
    fun bindSessionManager(sessionManagerImpl: SessionManagerImpl): SessionManager

    @Binds
    @Singleton
    fun bindAuthManager(authManagerImpl: AuthManagerImpl): AuthManager

    @Binds
    @Singleton
    fun bindTotpManager(totpManagerImpl: TotpManagerImpl): TotpManager
}