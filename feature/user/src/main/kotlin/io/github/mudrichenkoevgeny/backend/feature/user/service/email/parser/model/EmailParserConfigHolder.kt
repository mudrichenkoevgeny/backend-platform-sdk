package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

/**
 * Runtime holder for [EmailParserConfig].
 *
 * This exists to decouple template parsing configuration from DI wiring and to allow host apps
 * to provide configuration early during startup (before constructing [EmailParserImpl]).
 *
 * When not configured, [get] returns a default [EmailParserConfig].
 */
object EmailParserConfigHolder {
    private var config: EmailParserConfig? = null

    fun set(config: EmailParserConfig) {
        this.config = config
    }

    fun get(): EmailParserConfig = config ?: EmailParserConfig()

    internal fun resetForTests() {
        config = null
    }
}