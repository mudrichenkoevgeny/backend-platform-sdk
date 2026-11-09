package io.github.mudrichenkoevgeny.backend.feature.user.di.module

import dagger.Binds
import dagger.Module
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.refreshtokenprovider.RefreshTokenProviderImpl
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.JwtTokenProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.tokenprovider.TokenProvider
import javax.inject.Singleton

/**
 * Binds core auth/security infrastructure for the user feature.
 *
 * Provides default implementations for:
 * - [AuthenticationProvider] via [JwtAuthenticationProvider]
 * - [RefreshTokenProvider] via [RefreshTokenProviderImpl]
 * - [TokenProvider] via [JwtTokenProvider]
 */
@Module
public interface UserAuthInfrastructureModule {
    @Binds
    @Singleton
    public fun bindAuthenticationProvider(jwtAuthenticationProvider: JwtAuthenticationProvider): AuthenticationProvider

    @Binds
    @Singleton
    public fun bindRefreshTokenProvider(refreshTokenProviderImpl: RefreshTokenProviderImpl): RefreshTokenProvider

    @Binds
    @Singleton
    public fun bindTokenProvider(jwtTokenProvider: JwtTokenProvider): TokenProvider
}
