package io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.mapper.convertPasswordPolicyFailToAppError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicyValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidatePasswordUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val passwordPolicyValidator: PasswordPolicyValidator
) {
    /**
     * Validates a password against the active security policy.
     *
     * **Security:**
     * - Retrieves the current [PasswordPolicy] from [securitySettingsProvider] to ensure
     *   validation is performed against the latest system requirements.
     * - Enforces complexity rules (e.g., length, character sets) via [passwordPolicyValidator].
     *
     * **Workflow:**
     * 1. Fetches the effective password policy.
     * 2. Validates the provided raw password string.
     * 3. Maps any validation failures to [SecurityError.PasswordTooWeak], including specific
     *    violation details for client-side feedback.
     *
     * @param password The raw password string to validate.
     * @return [AppResult.Success] if the password satisfies the policy, or [AppResult.Error]
     *         with [SecurityError.PasswordTooWeak].
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