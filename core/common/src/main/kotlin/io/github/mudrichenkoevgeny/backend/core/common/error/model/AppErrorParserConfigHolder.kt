package io.github.mudrichenkoevgeny.backend.core.common.error.model

import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser

/**
 * Static holder for [AppErrorParserConfig], decoupled from DI lifecycle.
 *
 * Allows the host application to set the config after startup (e.g. from env or config server),
 * so that the error parser can be (re)configured at runtime without rebuilding or restarting
 * the process. Consumers that need the config (e.g. a custom [AppErrorParser] or bootstrap code)
 * call [get]; the app sets it once via [set] during initialization or when config is refreshed.
 */
object AppErrorParserConfigHolder {
    private var config: AppErrorParserConfig? = null

    /**
     * Sets the active parser config (e.g. at startup or on config reload).
     */
    fun set(config: AppErrorParserConfig) {
        this.config = config
    }

    /**
     * Returns the current config, or default [AppErrorParserConfig] if none was [set].
     */
    fun get(): AppErrorParserConfig = config ?: AppErrorParserConfig()
}