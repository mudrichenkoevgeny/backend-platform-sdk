package io.github.mudrichenkoevgeny.backend.core.common.application.ratelimit

import io.github.mudrichenkoevgeny.backend.core.common.network.contract.CommonNetworkHttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.request.httpMethod

/**
 * Configures a global rate limit for all incoming requests with static values.
 *
 * - Uses the Ktor [RateLimit] plugin in global mode.
 * - Applies [rateLimit] requests per [rateLimitPeriodSeconds] seconds.
 * - Uses the `X-Forwarded-For` header or remote address as a key.
 * - Skips limiting for `OPTIONS` (preflight) requests.
 */
fun Application.configureGlobalRateLimit(
    rateLimit: Int,
    rateLimitPeriodSeconds: Int
) {
    configureGlobalRateLimit(
        getMaxRequestsPerPeriod = { rateLimit },
        getRateLimitPeriodSeconds = { rateLimitPeriodSeconds }
    )
}

/**
 * Configures a global rate limit for all incoming requests with dynamic suppliers.
 *
 * Automatically detects changes in [getMaxRequestsPerPeriod] and [getRateLimitPeriodSeconds]
 * when settings are updated at runtime.
 */
fun Application.configureGlobalRateLimit(
    getMaxRequestsPerPeriod: () -> Int,
    getRateLimitPeriodSeconds: () -> Int
) {
    val dynamicRateLimiter = DynamicRateLimiter(
        getMaxRequestsPerPeriod = getMaxRequestsPerPeriod,
        getRateLimitPeriodSeconds = getRateLimitPeriodSeconds
    )

    install(RateLimit) {
        global {
            rateLimiter { _, key ->
                dynamicRateLimiter.getLimiterForKey(key)
            }

            requestKey { call ->
                if (call.request.httpMethod == HttpMethod.Options) {
                    return@requestKey "options-preflight-skip"
                }

                call.request.headers[CommonNetworkHttpHeaders.X_FORWARDED_FOR]
                    ?: call.request.local.remoteAddress
            }
        }
    }
}