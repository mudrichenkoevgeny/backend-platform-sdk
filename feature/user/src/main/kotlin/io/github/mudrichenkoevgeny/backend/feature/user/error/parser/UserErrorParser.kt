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
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveDisplayNameTooLongMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveDisplayNameTooShortMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveNicknameTooLongMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveNicknameTooShortMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveTotalUserIdentifiersLimitReachedMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveUserIdentifierLimitReachedMessage
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.localization.resolveUserLockedMessage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for user-related errors.
 */
@Singleton
public class UserErrorParser @Inject constructor() : AppErrorSpecificParser {
    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse? {
        val userStrings = UserModuleStrings(locale = locale)
        val message = resolveMessage(code, args, locale, userStrings) ?: return null

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
    ): ApiErrorResponse? = getApiErrorResponse(
        errorId = appError.errorId,
        code = appError.code,
        args = appError.publicArgs,
        locale = locale
    )

    private fun resolveMessage(
        code: String,
        args: Map<String, Any>?,
        locale: String,
        userStrings: UserModuleStrings
    ): String? = when (code) {
        UserErrorCodes.INVALID_ACCESS_TOKEN -> userStrings.errors.invalidSession
        UserErrorCodes.ACCESS_TOKEN_EXPIRED -> userStrings.errors.accessTokenExpired
        UserErrorCodes.INVALID_REFRESH_TOKEN -> userStrings.errors.invalidSession
        UserErrorCodes.INVALID_SESSION -> userStrings.errors.invalidSession
        UserErrorCodes.USER_ROLE_NOT_ALLOWED -> userStrings.errors.missingPermissions
        UserErrorCodes.USER_MISSING_PERMISSIONS -> userStrings.errors.missingPermissions
        UserErrorCodes.USER_INSUFFICIENT_AUTHORITY_LEVEL -> userStrings.errors.missingPermissions
        UserErrorCodes.USER_ILLEGAL_ACCOUNT_STATUS -> userStrings.errors.forbidden
        UserErrorCodes.USER_BANNED -> userStrings.errors.banned
        UserErrorCodes.USER_READ_ONLY -> userStrings.errors.readOnly
        UserErrorCodes.USER_SECURITY_HOLD -> userStrings.errors.securityHold
        UserErrorCodes.USER_PENDING_DELETION -> userStrings.errors.pendingDeletion
        UserErrorCodes.USER_FORBIDDEN -> userStrings.errors.forbidden
        UserErrorCodes.USER_NOT_FOUND -> userStrings.errors.notFound
        UserErrorCodes.INVALID_CREDENTIALS -> userStrings.errors.invalidCredentials
        UserErrorCodes.CAN_NOT_DELETE_USER_IDENTIFIER -> userStrings.errors.canNotDeleteIdentifier
        UserErrorCodes.CAN_NOT_CREATE_USER_IDENTIFIER -> userStrings.errors.canNotCreateIdentifier
        UserErrorCodes.WRONG_PASSWORD -> userStrings.errors.wrongPassword
        UserErrorCodes.WRONG_CONFIRMATION_CODE -> userStrings.errors.wrongConfirmationCode
        UserErrorCodes.EXTERNAL_IDENTIFIER_LINKAGE_FAILED -> userStrings.errors.externalLinkageFailed
        UserErrorCodes.REGISTRATION_DISABLED -> userStrings.errors.registrationDisabled
        UserErrorCodes.SELF_SERVICE_UNLOCK_DISABLED -> userStrings.errors.selfServiceUnlockDisabled
        UserErrorCodes.EMAIL_NOT_ALLOWED -> userStrings.errors.emailNotAllowed
        UserErrorCodes.PASSWORD_SETUP_REQUIRED -> userStrings.errors.passwordSetupRequired
        UserErrorCodes.USER_IDENTIFIER_PASSWORD_NOT_SUPPORTED -> userStrings.errors.identifierPasswordNotSupported
        UserErrorCodes.USER_IDENTIFIER_PASSWORD_NOT_SET -> userStrings.errors.identifierPasswordNotSet

        UserErrorCodes.USER_LOCKED -> {
            val lockoutTypeRaw = args?.get(UserErrorArgs.ACCOUNT_LOCKOUT_TYPE)?.toString()
            val lockoutType = lockoutTypeRaw?.let { AccountLockoutType.fromValueOrNull(it) }
            val lockoutUntilRaw = args?.get(UserErrorArgs.TEMPORARY_LOCKOUT_UNTIL)
            val lockoutUntilEpoch = lockoutUntilRaw?.toString()?.toLongOrNull()
            val formattedUntil = lockoutUntilEpoch?.formatEpochMillisToUtcString() ?: lockoutUntilRaw?.toString()
            userStrings.resolveUserLockedMessage(
                lockoutType = lockoutType,
                formattedUntil = formattedUntil
            )
        }

        UserErrorCodes.USER_IDENTIFIER_LIMIT_REACHED -> {
            val commonStrings = CommonModuleStrings(locale = locale)
            val limit = args?.get(UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS)
            val providerRaw = args?.get(UserErrorArgs.USER_AUTH_PROVIDER)?.toString()
            userStrings.resolveUserIdentifierLimitReachedMessage(
                commonStrings,
                limit = limit,
                providerRaw = providerRaw
            )
        }

        UserErrorCodes.TOTAL_USER_IDENTIFIERS_LIMIT_REACHED -> {
            val limit = args?.get(UserErrorArgs.MAX_NUMBER_OF_IDENTIFIERS)
            userStrings.resolveTotalUserIdentifiersLimitReachedMessage(limit = limit)
        }

        UserErrorCodes.NICKNAME_ALREADY_TAKEN -> userStrings.errors.nicknameAlreadyTaken
        UserErrorCodes.NICKNAME_CHANGE_DISABLED -> userStrings.errors.nicknameChangeDisabled
        UserErrorCodes.NICKNAME_TOO_SHORT -> {
            val minLength = args?.get(UserErrorArgs.NICKNAME_MIN_LENGTH)
            userStrings.resolveNicknameTooShortMessage(minLength = minLength)
        }

        UserErrorCodes.NICKNAME_TOO_LONG -> {
            val maxLength = args?.get(UserErrorArgs.NICKNAME_MAX_LENGTH)
            userStrings.resolveNicknameTooLongMessage(maxLength = maxLength)
        }

        UserErrorCodes.INVALID_NICKNAME_CHARACTERS -> userStrings.errors.invalidNicknameCharacters
        UserErrorCodes.DISPLAY_NAME_TOO_SHORT -> {
            val minLength = args?.get(UserErrorArgs.DISPLAY_NAME_MIN_LENGTH)
            userStrings.resolveDisplayNameTooShortMessage(minLength = minLength)
        }

        UserErrorCodes.DISPLAY_NAME_TOO_LONG -> {
            val maxLength = args?.get(UserErrorArgs.DISPLAY_NAME_MAX_LENGTH)
            userStrings.resolveDisplayNameTooLongMessage(maxLength = maxLength)
        }

        UserErrorCodes.INVALID_DISPLAY_NAME_CHARACTERS -> userStrings.errors.invalidDisplayNameCharacters

        else -> null
    }
}
