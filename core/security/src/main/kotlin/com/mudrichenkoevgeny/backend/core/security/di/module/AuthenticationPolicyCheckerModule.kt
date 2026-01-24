package com.mudrichenkoevgeny.backend.core.security.di.module

import com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyChecker
import com.mudrichenkoevgeny.backend.core.security.authenticationpolicychecker.AuthenticationPolicyCheckerImpl
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