package io.github.mudrichenkoevgeny.backend.feature.user.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.common.util.formatEpochMillisToUtcString
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.UserModuleStrings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for user-related errors.
 */
@Singleton
class UserErrorParser @Inject constructor() : AppErrorSpecificParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse? {
        val strings = UserModuleStrings(locale = locale).errors

        val message = when (code) {
            UserErrorCodes.INVALID_ACCESS_TOKEN -> strings.invalidAccessToken
            UserErrorCodes.ACCESS_TOKEN_EXPIRED -> strings.accessTokenExpired
            UserErrorCodes.INVALID_REFRESH_TOKEN -> strings.invalidRefreshToken
            UserErrorCodes.INVALID_SESSION -> strings.invalidSession
            UserErrorCodes.USER_ROLE_NOT_ALLOWED -> strings.roleNotAllowed
            UserErrorCodes.USER_MISSING_PERMISSIONS -> strings.missingPermissions
            UserErrorCodes.USER_INSUFFICIENT_AUTHORITY_LEVEL -> strings.insufficientAuthority
            UserErrorCodes.USER_ILLEGAL_ACCOUNT_STATUS -> strings.illegalStatus
            UserErrorCodes.USER_BANNED -> strings.banned
            UserErrorCodes.USER_READ_ONLY -> strings.readOnly
            UserErrorCodes.USER_SECURITY_HOLD -> strings.securityHold
            UserErrorCodes.USER_PENDING_DELETION -> strings.pendingDeletion
            UserErrorCodes.USER_FORBIDDEN -> strings.forbidden
            UserErrorCodes.USER_NOT_FOUND -> strings.notFound
            UserErrorCodes.INVALID_CREDENTIALS -> strings.invalidCredentials
            UserErrorCodes.CAN_NOT_DELETE_USER_IDENTIFIER -> strings.canNotDeleteIdentifier
            UserErrorCodes.CAN_NOT_CREATE_USER_IDENTIFIER -> strings.canNotCreateIdentifier
            UserErrorCodes.WRONG_PASSWORD -> strings.wrongPassword
            UserErrorCodes.WRONG_CONFIRMATION_CODE -> strings.wrongConfirmationCode
            UserErrorCodes.EXTERNAL_IDENTIFIER_LINKAGE_FAILED -> strings.externalLinkageFailed
            UserErrorCodes.REGISTRATION_DISABLED -> strings.registrationDisabled
            UserErrorCodes.SELF_SERVICE_UNLOCK_DISABLED -> strings.selfServiceUnlockDisabled
            UserErrorCodes.EMAIL_NOT_ALLOWED -> strings.emailNotAllowed
            UserErrorCodes.PASSWORD_SETUP_REQUIRED -> strings.passwordSetupRequired
            UserErrorCodes.USER_IDENTIFIER_PASSWORD_NOT_SUPPORTED -> strings.identifierPasswordNotSupported
            UserErrorCodes.USER_IDENTIFIER_PASSWORD_NOT_SET -> strings.identifierPasswordNotSet

            UserErrorCodes.USER_LOCKED -> {
                val lockoutUntilRaw = args?.get(UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL)
                val lockoutUntilEpoch = lockoutUntilRaw?.toString()?.toLongOrNull()
                val formattedUntil = lockoutUntilEpoch?.formatEpochMillisToUtcString() ?: lockoutUntilRaw?.toString()
                if (formattedUntil != null) {
                    strings.locked(formattedUntil)
                } else {
                    strings.lockedWithoutArgs
                }
            }

            UserErrorCodes.USER_IDENTIFIER_LIMIT_REACHED -> {
                val max = args?.get(UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS)
                val provider = args?.get(UserErrorArgs.USER_AUTH_PROVIDER)
                if (max != null && provider != null) {
                    strings.identifierLimitReached(max, provider)
                } else {
                    strings.identifierLimitReachedWithoutArgs
                }
            }

            UserErrorCodes.TOTAL_USER_IDENTIFIERS_LIMIT_REACHED -> {
                val max = args?.get(UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS)
                if (max != null) {
                    strings.totalIdentifiersLimitReached(max)
                } else {
                    strings.totalIdentifiersLimitReachedWithoutArgs
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
        val effectiveArgs = if (appError.code == UserErrorCodes.USER_LOCKED) {
            val lockoutUntilEpoch = appError.publicArgs?.get(UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL)
                ?.toString()?.toLongOrNull()
            if (lockoutUntilEpoch != null) {
                val formattedDate = lockoutUntilEpoch.formatEpochMillisToUtcString()
                val newArgs = appError.publicArgs?.toMutableMap() ?: mutableMapOf()
                newArgs[UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL] = formattedDate
                newArgs
            } else {
                appError.publicArgs
            }
        } else {
            appError.publicArgs
        }

        return getApiErrorResponse(
            errorId = appError.errorId,
            code = appError.code,
            args = effectiveArgs,
            locale = locale
        )
    }
}