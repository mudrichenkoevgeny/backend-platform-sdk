package io.github.mudrichenkoevgeny.backend.core.security.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.naming.CommonErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.core.security.error.naming.SecurityErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.core.security.localization.SecurityModuleStrings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for security-related errors (TOTP, MFA, password policy, OTP retry, etc.).
 */
@Singleton
class SecurityErrorParser @Inject constructor() : AppErrorSpecificParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse? {
        val strings = SecurityModuleStrings(locale = locale).errors

        val message = when (code) {
            SecurityErrorCodes.TOTP_ALREADY_ENABLED -> strings.totpAlreadyEnabled
            SecurityErrorCodes.TOTP_NOT_ENABLED -> strings.totpNotEnabled
            SecurityErrorCodes.MFA_TOKEN_EXPIRED -> strings.mfaTokenExpired
            SecurityErrorCodes.INVALID_MFA_TOKEN -> strings.invalidMfaToken
            SecurityErrorCodes.INVALID_TOTP_CODE -> strings.invalidTotpCode
            SecurityErrorCodes.RECOVERY_CODE_ALREADY_USED -> strings.recoveryCodeAlreadyUsed
            SecurityErrorCodes.IP_NOT_ALLOWED -> strings.ipNotAllowed

            SecurityErrorCodes.MFA_CONFIRMATION_REQUIRED -> {
                val mfaToken = args?.get(SecurityErrorArgs.MFA_TOKEN)
                if (mfaToken != null) {
                    strings.mfaConfirmationRequired(mfaToken)
                } else {
                    strings.mfaConfirmationRequiredWithoutArgs
                }
            }

            SecurityErrorCodes.OTP_RETRY_TOO_SOON -> {
                val retryAfterSeconds = args?.get(CommonErrorArgs.RETRY_AFTER_SECONDS)
                if (retryAfterSeconds != null) {
                    strings.otpRetryTooSoon(retryAfterSeconds)
                } else {
                    strings.otpRetryTooSoonWithoutArgs
                }
            }

            SecurityErrorCodes.PASSWORD_TOO_WEAK -> {
                val minLength = args?.get(SecurityErrorArgs.PASSWORD_MIN_LENGTH)
                if (minLength != null) {
                    strings.passwordTooShort(minLength)
                } else {
                    strings.passwordTooWeakWithoutArgs
                }
            }

            else -> return null
        }

        return ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = code,
            message = message,
            args = args?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    override fun getApiErrorResponse(
        appError: AppError,
        locale: String
    ): ApiErrorResponse? {
        return getApiErrorResponse(
            errorId = appError.errorId,
            code = appError.code,
            args = appError.publicArgs,
            locale = locale
        )
    }
}