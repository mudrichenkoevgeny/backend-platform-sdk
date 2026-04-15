package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailParserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailTemplate
import io.github.mudrichenkoevgeny.shared.foundation.core.common.serialization.FoundationJson
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classpath-resource-based [EmailParser] implementation.
 *
 * At construction time it scans configured resource locations for locale-specific JSON maps:
 * `"{path}/{locale}/{fileName}.{extension}"`, and keeps an in-memory cache:
 * `locale -> (templateKey -> EmailTemplate)`.
 *
 * Lookup fallbacks:
 * - exact normalized locale (lowercased)
 * - language-only (e.g. `en` for `en-US`)
 * - default locale (`en`)
 */
@Singleton
class EmailParserImpl @Inject constructor(
    config: EmailParserConfig
) : EmailParser {

    private val json = FoundationJson
    private val cache: MutableMap<String, MutableMap<String, EmailTemplate>> = mutableMapOf()
    private val defaultLocale = "en"

    init {
        val classLoader = Thread.currentThread().contextClassLoader

        for (locale in config.supportedLocales) {
            val localeTemplates = mutableMapOf<String, EmailTemplate>()

            for (path in config.resourcePaths) {
                val resourceName = "$path/$locale/${config.resourceFileName}.${config.resourceFileExtension}"
                val resourceStream = classLoader.getResourceAsStream(resourceName) ?: continue

                resourceStream.use { stream ->
                    val text = stream.bufferedReader().readText().trim()
                    if (text.isEmpty() || text == "{}" || text == "null") return@use

                    val parsed: Map<String, EmailTemplate> = json.decodeFromString(text)
                    localeTemplates.putAll(parsed)
                }
            }

            if (localeTemplates.isNotEmpty()) {
                cache[locale.lowercase()] = localeTemplates
            }
        }
    }

    override fun getTemplate(key: String, args: Map<String, Any>?, locale: String): EmailTemplate? {
        val normalizedLocale = locale.lowercase()
        val languageOnly = normalizedLocale.split("-")[0].split("_")[0]

        val templatesForLocale = cache[normalizedLocale]
            ?: cache[languageOnly]
            ?: cache[defaultLocale]
            ?: return null

        val template = templatesForLocale[key]
            ?: cache[defaultLocale]?.get(key)
            ?: return null

        if (args.isNullOrEmpty()) {
            return template
        }

        return EmailTemplate(
            subject = replaceArgs(template.subject, args),
            body = replaceArgs(template.body, args)
        )
    }

    private fun replaceArgs(text: String, args: Map<String, Any>): String {
        return args.entries.fold(text) { current, (key, value) ->
            current.replace("{$key}", value.toString())
        }
    }
}