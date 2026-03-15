package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model.EmailTemplate

interface EmailParser {
    fun getTemplate(key: String, args: Map<String, Any>?, locale: String): EmailTemplate?
}