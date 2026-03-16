package io.github.mudrichenkoevgeny.backend.core.common.network.cors

/**
 * Shared CORS configuration values for HTTP endpoints.
 */
object CorsConfig {

    /**
     * Default `Access-Control-Max-Age` value (in seconds) used for preflight caching.
     */
    const val CORS_MAX_AGE_SECONDS = 3600L
}