package io.github.mudrichenkoevgeny.backend.core.security.di.module

import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyCheckerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface AuthenticationPolicyCheckerModule {

    @Binds
    @Singleton
    fun bindAuthenticationPolicyChecker(
        authenticationPolicyCheckerImpl: AuthenticationPolicyCheckerImpl
    ): AuthenticationPolicyChecker
}