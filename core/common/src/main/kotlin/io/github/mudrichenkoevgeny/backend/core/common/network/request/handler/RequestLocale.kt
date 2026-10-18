package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.shared.foundation.core.common.localization.CommonLocales
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.acceptLanguageItems

/**
 * Extracts the user's preferred locale from the `Accept-Language` HTTP header
 * considering HTTP quality factors (`q=`), defaulting to [CommonLocales.DEFAULT] if omitted or invalid.
 */
fun ApplicationCall.requestLocale(): String {
    return request.acceptLanguageItems()
        .asSequence()
        .map { it.value.substringBefore('-') }
        .firstOrNull { it in CommonLocales.SUPPORTED }
        ?: CommonLocales.DEFAULT
}