package io.github.mudrichenkoevgeny.backend.core.security.di.module

import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.PasswordPolicyChecker
import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.PasswordPolicyCheckerImpl
import io.github.mudrichenkoevgeny.backend.core.security.passwordpolicychecker.model.PasswordPolicy
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