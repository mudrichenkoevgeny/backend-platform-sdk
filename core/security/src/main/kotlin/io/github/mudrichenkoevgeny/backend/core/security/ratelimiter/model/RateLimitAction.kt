package io.github.mudrichenkoevgeny.backend.core.security.ratelimiter.model

/**
 * Contract for a rate-limited logical action.
 *
 * Feature modules define concrete implementations (typically enums) with stable [id] values used
 * in storage keys and error payloads, plus [limit] and [windowSeconds] for the sliding window.
 *
 * Default [createKey] follows `rl:{id}:{identifier}`; override when a different key shape is required.
 */
interface RateLimitAction {
    val id: String
    val limit: Int
    val windowSeconds: Int

    fun createKey(identifier: String): String = "rl:$id:$identifier"
}
