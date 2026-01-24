package com.mudrichenkoevgeny.backend.core.security.di.module

import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.PasswordPolicyChecker
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.PasswordPolicyCheckerImpl
import com.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.model.PasswordPolicy
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
interface PasswordPolicyCheckerModule {

    @Binds
    @Singleton
    fun bindPasswordPolicyChecker(passwordPolicyCheckerImpl: PasswordPolicyCheckerImpl): PasswordPolicyChecker

    companion object {
        @Provides
        @Singleton
        fun providePasswordPolicy(): PasswordPolicy {
            return PasswordPolicy()
        }
    }
}