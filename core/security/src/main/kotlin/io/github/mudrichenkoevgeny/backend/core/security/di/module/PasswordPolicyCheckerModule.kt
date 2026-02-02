package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicychecker.PasswordPolicyChecker
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicychecker.PasswordPolicyCheckerImpl
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicychecker.model.PasswordPolicy
import javax.inject.Singleton

@Module
class PasswordPolicyCheckerModule {

    @Provides
    @Singleton
    fun providePasswordPolicy(): PasswordPolicy {
        return PasswordPolicy()
    }

    @Provides
    @Singleton
    fun providePasswordPolicyChecker(passwordPolicy: PasswordPolicy): PasswordPolicyChecker {
        return PasswordPolicyCheckerImpl(passwordPolicy)
    }
}