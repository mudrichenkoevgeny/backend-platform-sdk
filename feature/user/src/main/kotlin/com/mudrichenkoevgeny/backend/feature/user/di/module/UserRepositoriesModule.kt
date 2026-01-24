package com.mudrichenkoevgeny.backend.feature.user.di.module

import com.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import com.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepositoryImpl
import com.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepository
import com.mudrichenkoevgeny.backend.feature.user.database.repository.useridentifier.UserIdentifierRepositoryImpl
import com.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import com.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepositoryImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

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
}