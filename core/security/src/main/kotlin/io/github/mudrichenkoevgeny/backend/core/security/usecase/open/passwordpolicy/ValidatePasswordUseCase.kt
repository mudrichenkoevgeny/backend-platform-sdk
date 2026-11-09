package io.github.mudrichenkoevgeny.backend.core.security.usecase.open.passwordpolicy

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.mapper.convertPasswordPolicyFailToAppError
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.ManagementPasswordPolicy
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.passwordpolicy.PasswordPolicyValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Open use case for validating raw passwords against the active platform password policy.
 *
 * Enforces security constraints (length, character sets, complexity) on passwords submitted during
 * registration, password resets, or credential updates.
 */
@Singleton
public class ValidatePasswordUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val passwordPolicyValidator: PasswordPolicyValidator
) {
    /**
     * Validates a raw password against the active management password policy.
     *
     * **Allowed Account Statuses:** Any (Public access).
     *
     * **Security:**
     * - Evaluates passwords against the latest [ManagementPasswordPolicy] fetched from [SecuritySettingsProvider].
     * - Does not persist or log raw password values.
     *
     * **Workflow:**
     * 1. Fetches the active platform password policy from [securitySettingsProvider].
     * 2. Evaluates the raw password using [passwordPolicyValidator].
     * 3. Maps complexity failures to [SecurityError.PasswordTooWeak] with specific violation details for client-side feedback.
     *
     * @param password The raw password string to validate.
     * @return [AppResult.Success] if valid, or [AppResult.Error] with [SecurityError.PasswordTooWeak] on policy failure.
     */
    public operator fun invoke(password: String): AppResult<Unit> {
        val passwordPolicy = securitySettingsProvider.getManagementPasswordPolicy()
        val validationResult = passwordPolicyValidator.validate(
            passwordPolicy,
            password
        )

        return when (validationResult) {
            is PasswordPolicyValidatorResult.Success -> AppResult.Success(Unit)
            is PasswordPolicyValidatorResult.Fail -> {
                AppResult.Error(validationResult.convertPasswordPolicyFailToAppError())
            }
        }
    }
}
