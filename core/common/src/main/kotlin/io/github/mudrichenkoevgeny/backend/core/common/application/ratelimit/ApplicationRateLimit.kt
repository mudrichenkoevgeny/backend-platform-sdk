package io.github.mudrichenkoevgeny.backend.core.common.application.ratelimit

import io.github.mudrichenkoevgeny.backend.core.common.network.contract.CommonNetworkHttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.request.httpMethod
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import kotlin.time.Duration.Companion.seconds

fun Application.configureGlobalRateLimit(
    rateLimit: Int,
    rateLimitPeriodSeconds: Int
) {
    install(RateLimit) {
        global {
            rateLimiter(
                limit = rateLimit,
                refillPeriod = rateLimitPeriodSeconds.seconds
            )

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