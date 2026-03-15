package io.github.mudrichenkoevgeny.backend.core.common.error.parser

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.ErrorId
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppErrorParserImpl @Inject constructor(
    appErrorParserConfig: AppErrorParserConfig
) : AppErrorParser {

    private val json = FoundationJson
    private val cache: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    companion object {
        private const val DEFAULT_LOCALE = "en"
        private const val UNKNOWN_ERROR_MESSAGE = "Unknown error"
    }

    init {
        val classLoader = Thread.currentThread().contextClassLoader

        for (locale in appErrorParserConfig.supportedLocales) {
            val localeMessages = mutableMapOf<String, String>()

            for (path in appErrorParserConfig.resourcePaths) {
                val resourceName = "$path/$locale/${appErrorParserConfig.resourceFileName}" +
                        ".${appErrorParserConfig.resourceFileExtension}"

                val resourceStream = classLoader.getResourceAsStream(resourceName) ?: continue

                resourceStream.use { stream ->
                    val text = stream.bufferedReader().readText().trim()

                    if (text.isEmpty() || text == "{}" || text == "null") {
                        return@use
                    }

                    try {
                        val parsed: Map<String, String> = json.decodeFromString(text)
                        localeMessages.putAll(parsed)
                    } catch (_: Exception) { }
                }
            }

            if (localeMessages.isNotEmpty()) {
                cache[locale.lowercase()] = localeMessages
            }
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

        val normalizedLocale = locale.lowercase()
        val languageOnly = normalizedLocale.split("-")[0].split("_")[0]

        val messagesForLocale = cache[normalizedLocale]
            ?: cache[languageOnly]
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