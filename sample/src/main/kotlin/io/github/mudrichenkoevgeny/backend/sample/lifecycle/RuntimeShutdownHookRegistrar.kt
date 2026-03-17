package io.github.mudrichenkoevgeny.backend.sample.lifecycle

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production [ShutdownHookRegistrar] backed by [Runtime.getRuntime].
 */
@Singleton
class RuntimeShutdownHookRegistrar @Inject constructor() : ShutdownHookRegistrar {
    override fun addShutdownHook(hook: Thread) {
        Runtime.getRuntime().addShutdownHook(hook)
    }
}

