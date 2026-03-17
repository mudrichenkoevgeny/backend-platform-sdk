package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailTemplate

/**
 * Resolves localized email templates and performs argument substitution.
 *
 * Templates are addressed by a stable key (e.g. `verification_code`) and selected using the requested locale.
 * Implementations may provide locale fallback (language-only or default locale) and may cache templates for speed.
 */
interface EmailParser {
    /**
     * Returns the [EmailTemplate] for [key] and [locale], with optional placeholder replacement.
     *
     * Placeholders in templates use `{argKey}` format and are replaced using [args].
     *
     * @param key template identifier
     * @param args optional placeholder values
     * @param locale requested locale string (e.g. `en`, `en-US`, `ru_RU`)
     */
    fun getTemplate(key: String, args: Map<String, Any>?, locale: String): EmailTemplate?
}