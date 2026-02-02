package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProviderImpl
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.JwtTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface UserAuthInfrastructureModule {

    @Binds
    @Singleton
    fun bindAuthenticationProvider(jwtAuthenticationProvider: JwtAuthenticationProvider): AuthenticationProvider

    @Binds
    @Singleton
    fun bindRefreshTokenProvider(refreshTokenProviderImpl: RefreshTokenProviderImpl): RefreshTokenProvider

    @Binds
    @Singleton
    fun bindTokenProvider(jwtTokenProvider: JwtTokenProvider): TokenProvider
}