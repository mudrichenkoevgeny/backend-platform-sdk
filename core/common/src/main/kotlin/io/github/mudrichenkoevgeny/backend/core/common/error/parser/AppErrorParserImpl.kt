package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.ApiError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.backend.core.common.serialization.DefaultJson
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppErrorParserImpl @Inject constructor(
    appErrorParserConfig: AppErrorParserConfig
) : AppErrorParser {

    private val json = DefaultJson
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

    override fun getApiError(errorId: ErrorId, code: String, args: Map<String, Any>?, locale: String): ApiError {
        return ApiError(
            id = errorId.value.toString(),
            code = code,
            message = parseError(code, args, locale),
            args = args?.mapValues { it.value.toString() } ?: emptyMap()
        )
    }

    override fun getApiError(appError: AppError, locale: String): ApiError {
        return ApiError(
            id = appError.errorId.value.toString(),
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