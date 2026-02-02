package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppErrorParserImpl @Inject constructor(
    appErrorParserConfig: AppErrorParserConfig
) : AppErrorParser {

    private val json = FoundationJson
    private val cache: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    init {
        val classLoader = Thread.currentThread().contextClassLoader

        for (locale in appErrorParserConfig.supportedLocales) {
            val localeMessages = mutableMapOf<String, String>()

            for (path in appErrorParserConfig.resourcePaths) {
                val resourceName = "$path/${appErrorParserConfig.resourceFileNamePrefix}$locale" +
                        ".${appErrorParserConfig.resourceFileExtension}"
                val resourceStream = classLoader.getResourceAsStream(resourceName) ?: continue

                val text = resourceStream.bufferedReader().readText()
                val parsed: Map<String, String> = json.decodeFromString(text)

                localeMessages.putAll(parsed)
            }

            cache[locale] = localeMessages
        }
    }

    override fun getApiErrorResponse(
        errorId: ErrorId,
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): ApiErrorResponse {
        return ApiErrorResponse(
            id = errorId.asHexDashString(),
            code = code,
            message = parseError(code, args, locale),
            args = args?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    override fun getApiErrorResponse(appError: AppError, locale: String): ApiErrorResponse {
        return ApiErrorResponse(
            id = appError.errorId.asHexDashString(),
            code = appError.code,
            message = parseError(appError.code, appError.publicArgs, locale),
            args = appError.publicArgs?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    private fun parseError(
        code: String,
        args: Map<String, Any>?,
        locale: String
    ): String {
        if (cache.isEmpty()) {
            return UNKNOWN_ERROR_MESSAGE
        }

        val normalizedLocale = locale.lowercase(Locale.getDefault())
        val messagesForLocale = cache[normalizedLocale]
            ?: cache[DEFAULT_LOCALE]
            ?: emptyMap()

        val message = messagesForLocale[code]
            ?: cache[DEFAULT_LOCALE]?.get(code)
            ?: return UNKNOWN_ERROR_MESSAGE

        return if (args.isNullOrEmpty()) {
            message
        } else {
            args.entries.fold(message) { messageWithArgs, (key, value) ->
                messageWithArgs.replace("{$key}", value.toString())
            }
        }
    }
}