package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.auth.AuthManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.session.SessionManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.user.UserManagerImpl
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManager
import io.github.mudrichenkoevgeny.backend.feature.user.manager.useridentifier.UserIdentifierManagerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface UserManagersModule {

    @Binds
    @Singleton
    fun bindUserManager(userManagerImpl: UserManagerImpl): UserManager

    @Binds
    @Singleton
    fun bindUserIdentifierManager(userIdentifierManagerImpl: UserIdentifierManagerImpl): UserIdentifierManager

    @Binds
    @Singleton
    fun bindSessionManager(sessionManagerImpl: SessionManagerImpl): SessionManager

    @Binds
    @Singleton
    fun bindAuthManager(authManagerImpl: AuthManagerImpl): AuthManager
}