package io.github.mudrichenkoevgeny.backend.core.security.di.module

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidator
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidatorImpl
import javax.inject.Singleton

/**
 * Dagger module that provides password policy and its validator.
 *
 * - Exposes the default [ManagementPasswordPolicy] from [SecurityConfig] as a dependency.
 * - Provides [PasswordPolicyValidator] as [PasswordPolicyValidatorImpl].
 */
@Module
class PasswordPolicyValidatorModule {

    @Provides
    @Singleton
    fun provideManagementPasswordPolicy(securityConfig: SecurityConfig): ManagementPasswordPolicy {
        return securityConfig.managementSecuritySettings.passwordPolicy
    }

    @Provides
    @Singleton
    fun providePasswordPolicyValidator(): PasswordPolicyValidator {
        return PasswordPolicyValidatorImpl()
    }
}
