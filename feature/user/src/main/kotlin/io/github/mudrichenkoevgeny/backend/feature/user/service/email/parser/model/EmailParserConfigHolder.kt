package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

object EmailParserConfigHolder {
    private var config: EmailParserConfig? = null

    fun set(config: EmailParserConfig) {
        this.config = config
    }

    fun get(): EmailParserConfig = config ?: EmailParserConfig()
}