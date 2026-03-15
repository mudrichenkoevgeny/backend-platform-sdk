package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse

const val DEFAULT_LOCALE = "en"
const val UNKNOWN_ERROR_MESSAGE = "Unknown error"

/**
 * Provides localized messages for application errors (AppError).
 *
 * This class loads JSON resource files containing error messages for multiple locales,
 * caches them, and allows retrieving messages by error code and locale, optionally
 * formatting them with arguments.
 */
interface AppErrorParser {

    /**
     * Builds a client-facing API error response by error code and optional arguments.
     *
     * Message is resolved from localization by [code] and [locale]; placeholders in the
     * message template (e.g. `{fieldName}`) are replaced with values from [args].
     *
     * @param errorId Unique id for this error instance (e.g. for logging and support).
     * @param code Stable error code used as key in localization (e.g. `MISSING_REQUIRED_FIELD`).
     * @param args Optional map of placeholder names to values; only these are exposed in the response.
     * @param locale Preferred locale for the message (e.g. `en`, `ru`); falls back to [DEFAULT_LOCALE] if missing.
     * @return [ApiErrorResponse] with localized message and [args] for the client.
     */
    fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>? = null,
        locale: String = DEFAULT_LOCALE
    ): ApiErrorResponse

    /**
     * Builds a client-facing API error response from an [AppError].
     *
     * - [AppError.errorId] is copied to the response `id` (for support and correlation).
     * - [AppError.code] is copied to the response and used as the key to look up the message
     *   in the localization file for [locale]; e.g. code `MISSING_REQUIRED_FIELD` → string from JSON.
     * - [AppError.publicArgs] are copied to the response and used to replace placeholders in the
     *   message template (e.g. `{fieldName}` → value). Only these args are sent to the client.
     * - [AppError.secretArgs] are never included in the response; they are for server-side logging only.
     *
     * @param appError The domain error implementing [AppError].
     * @param locale Preferred locale for the message (e.g. `en`, `ru`); falls back to [DEFAULT_LOCALE] if missing.
     * @return [ApiErrorResponse] with localized message and public args for the client.
     */
    fun getApiErrorResponse(
        appError: AppError,
        locale: String = DEFAULT_LOCALE
    ): ApiErrorResponse
}