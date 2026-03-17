package io.github.mudrichenkoevgeny.backend.sample.lifecycle

/**
 * Abstraction for JVM shutdown hook registration.
 *
 * This indirection keeps [AppShutdownHookImpl] testable without relying on global JVM runtime state.
 */
interface ShutdownHookRegistrar {
    /**
     * Registers a new shutdown [hook] thread to be executed on JVM termination.
     */
    fun addShutdownHook(hook: Thread)
}

