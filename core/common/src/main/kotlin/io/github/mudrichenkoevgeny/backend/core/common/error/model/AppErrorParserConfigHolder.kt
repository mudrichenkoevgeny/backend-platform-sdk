package io.github.mudrichenkoevgeny.backend.core.common.error.model

object AppErrorParserConfigHolder {
    private var config: AppErrorParserConfig? = null

    fun set(config: AppErrorParserConfig) {
        this.config = config
    }

    fun get(): AppErrorParserConfig = config ?: AppErrorParserConfig()
}