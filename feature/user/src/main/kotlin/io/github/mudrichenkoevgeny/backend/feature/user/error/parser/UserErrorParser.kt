package io.github.mudrichenkoevgeny.backend.feature.user.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorSpecificParser
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.CommonErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.util.formatEpochMillisToUtcString
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorArgs
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.error.naming.UserErrorCodes
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feature-specific error parser for user-related errors.
 */
@Singleton
class UserErrorParser @Inject constructor(
    private val commonParser: CommonErrorParser,
) : AppErrorSpecificParser {

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String,
    ): ApiErrorResponse? {
        return null
    }

    override fun getApiErrorResponse(
        appError: AppError,
        locale: String,
    ): ApiErrorResponse? {
        if (appError.code == UserErrorCodes.USER_BLOCKED) {
            val blockedUntilEpoch = appError.publicArgs?.get(UserErrorArgs.BLOCKED_UNTIL)?.toString()?.toLongOrNull()
            if (blockedUntilEpoch != null) {
                val formattedDate = blockedUntilEpoch.formatEpochMillisToUtcString()
                
                val newArgs = appError.publicArgs?.toMutableMap() ?: mutableMapOf()
                newArgs[UserErrorArgs.BLOCKED_UNTIL] = formattedDate

                return commonParser.getApiErrorResponse(
                    errorId = appError.errorId,
                    code = appError.code,
                    args = newArgs,
                    locale = locale
                )
            }
        }
        return null
    }
}
