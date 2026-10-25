package io.github.mudrichenkoevgeny.backend.feature.user.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.common.util.formatEpochMillisToUtcString
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.localization.CommonModuleStrings
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.accountlockout.AccountLockoutType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.UserModuleStrings
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveTotalUserIdentifiersLimitReachedMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveUserIdentifierLimitReachedMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveUserLockedMessage
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
        val userStrings = UserModuleStrings(locale = locale)
        val strings = userStrings.errors

        val message = when (code) {
            UserErrorCodes.INVALID_ACCESS_TOKEN -> strings.invalidSession
            UserErrorCodes.ACCESS_TOKEN_EXPIRED -> strings.accessTokenExpired
            UserErrorCodes.INVALID_REFRESH_TOKEN -> strings.invalidSession
            UserErrorCodes.INVALID_SESSION -> strings.invalidSession
            UserErrorCodes.USER_ROLE_NOT_ALLOWED -> strings.missingPermissions
            UserErrorCodes.USER_MISSING_PERMISSIONS -> strings.missingPermissions
            UserErrorCodes.USER_INSUFFICIENT_AUTHORITY_LEVEL -> strings.missingPermissions
            UserErrorCodes.USER_ILLEGAL_ACCOUNT_STATUS -> strings.forbidden
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
                val lockoutTypeRaw = args?.get(UserErrorArgs.ACCOUNT_LOCKOUT_TYPE)?.toString()
                val lockoutType = lockoutTypeRaw?.let { AccountLockoutType.fromValueOrNull(it) }
                val lockoutUntilRaw = args?.get(UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL)
                val lockoutUntilEpoch = lockoutUntilRaw?.toString()?.toLongOrNull()
                val formattedUntil = lockoutUntilEpoch?.formatEpochMillisToUtcString() ?: lockoutUntilRaw?.toString()
                userStrings.resolveUserLockedMessage(lockoutType = lockoutType, formattedUntil = formattedUntil)
            }

            UserErrorCodes.USER_IDENTIFIER_LIMIT_REACHED -> {
                val commonStrings = CommonModuleStrings(locale = locale)
                val limit = args?.get(UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS)
                val providerRaw = args?.get(UserErrorArgs.USER_AUTH_PROVIDER)?.toString()
                userStrings.resolveUserIdentifierLimitReachedMessage(commonStrings, limit = limit, providerRaw = providerRaw)
            }

            UserErrorCodes.TOTAL_USER_IDENTIFIERS_LIMIT_REACHED -> {
                val limit = args?.get(UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS)
                userStrings.resolveTotalUserIdentifiersLimitReachedMessage(limit = limit)
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