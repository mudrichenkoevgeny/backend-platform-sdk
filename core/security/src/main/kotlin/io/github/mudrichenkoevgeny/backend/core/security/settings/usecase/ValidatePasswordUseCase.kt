package io.github.mudrichenkoevgeny.backend.core.security.settings.usecase

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.security.error.model.SecurityError
import io.github.mudrichenkoevgeny.backend.core.security.settings.provider.SecuritySettingsProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicyFailReason
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.model.PasswordPolicyValidatorResult
import io.github.mudrichenkoevgeny.shared.foundation.core.security.passwordpolicy.validator.PasswordPolicyValidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidatePasswordUseCase @Inject constructor(
    private val securitySettingsProvider: SecuritySettingsProvider,
    private val passwordPolicyValidator: PasswordPolicyValidator
) {
    operator fun invoke(password: String): AppResult<Unit> {
        val passwordPolicy = securitySettingsProvider.requirePasswordPolicy()
        val validationResult = passwordPolicyValidator.validate(passwordPolicy, password)

        return when (validationResult) {
            is PasswordPolicyValidatorResult.Success -> AppResult.Success(Unit)
            is PasswordPolicyValidatorResult.Fail -> {
                AppResult.Error(convertPasswordPolicyFailToAppError(validationResult))
            }
        }
    }
}

private fun convertPasswordPolicyFailToAppError(passwordPolicyValidatorFail: PasswordPolicyValidatorResult.Fail): SecurityError.PasswordTooWeak {
    val errorArgs: MutableMap<String, Any> = mutableMapOf()

    errorArgs[SecurityErrorArgs.PASSWORD_MIN_LENGTH] = passwordPolicyValidatorFail.passwordPolicy.minLength
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_TOO_SHORT] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.TOO_SHORT)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_LETTER] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.NO_LETTER)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_UPPERCASE] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.NO_UPPERCASE)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_LOWERCASE] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.NO_LOWERCASE)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_DIGIT] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.NO_DIGIT)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_NO_SPECIAL_CHAR] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.NO_SPECIAL_CHAR)
    errorArgs[SecurityErrorArgs.PASSWORD_FAIL_TOO_COMMON] = passwordPolicyValidatorFail.reasons.contains(PasswordPolicyFailReason.TOO_COMMON)

    return SecurityError.PasswordTooWeak(errorArgs)
}