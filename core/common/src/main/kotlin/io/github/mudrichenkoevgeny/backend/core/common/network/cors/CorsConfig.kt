package io.github.mudrichenkoevgeny.backend.core.common.network.cors

/**
 * Shared CORS configuration values for HTTP endpoints.
 */
public object CorsConfig {
    /**
     * Default `Access-Control-Max-Age` value (in seconds) used for preflight caching.
     */
    public const val CORS_MAX_AGE_SECONDS: Long = 3600L
}
