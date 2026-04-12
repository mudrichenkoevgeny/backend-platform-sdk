package io.github.mudrichenkoevgeny.backend.feature.security.api.usecase.open.passwordpolicy

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.mapper.convertPasswordPolicyFailToAppError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicyValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates a raw password against the currently effective password policy.
 *
 * On failure, maps validation reasons into [SecurityError.PasswordTooWeak] `publicArgs` so the
 * client can render a localized error message and show which rules were violated.
 */
@Singleton
class ValidatePasswordUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val passwordPolicyValidator: PasswordPolicyValidator
) {
    /**
     * Validates [password] and returns:
     * - [AppResult.Success] if it satisfies policy
     * - [AppResult.Error] with [SecurityError.PasswordTooWeak] otherwise
     */
    operator fun invoke(password: String): AppResult<Unit> {
        val passwordPolicy = securitySettingsProvider.getPasswordPolicy()
        val validationResult = passwordPolicyValidator.validate(passwordPolicy, password)

        return when (validationResult) {
            is PasswordPolicyValidatorResult.Success -> AppResult.Success(Unit)
            is PasswordPolicyValidatorResult.Fail -> {
                AppResult.Error(validationResult.convertPasswordPolicyFailToAppError())
            }
        }
    }
}