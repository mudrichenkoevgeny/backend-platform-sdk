package io.github.mudrichenkoevgeny.backend.core.security.di.module

import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import io.github.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyCheckerImpl
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

/**
 * Dagger bindings for authentication policy checks.
 *
 * Binds [AuthenticationPolicyChecker] to [AuthenticationPolicyCheckerImpl] (self-service and management re-auth windows).
 */
@Module
interface AuthenticationPolicyCheckerModule {

    @Binds
    @Singleton
    fun bindAuthenticationPolicyChecker(
        authenticationPolicyCheckerImpl: AuthenticationPolicyCheckerImpl
    ): AuthenticationPolicyChecker
}