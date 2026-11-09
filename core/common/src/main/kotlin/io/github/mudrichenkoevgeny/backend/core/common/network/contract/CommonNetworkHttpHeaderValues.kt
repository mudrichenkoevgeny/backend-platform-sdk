package io.github.mudrichenkoevgeny.backend.core.common.network.contract

/**
 * Common header values used for security-related HTTP response headers.
 */
public object CommonNetworkHttpHeaderValues {
    public const val SERVER_MASK_HEADER_VALUE: String = "Server"
    public const val NOSNIFF_HEADER_VALUE: String = "nosniff"
    public const val DENY_HEADER_VALUE: String = "DENY"
    public const val XSS_BLOCK_HEADER_VALUE: String = "1; mode=block"
    public const val CSP_API_HEADER_VALUE: String =
        "default-src 'self'; img-src 'self' https:; " +
            "frame-ancestors 'none';"
    public const val CSP_DEV_HEADER_VALUE: String =
        "default-src 'self' 'unsafe-inline'; connect-src 'self' " +
            "http://localhost:* ws://localhost:*; " +
            "img-src 'self' data: https:; frame-ancestors 'none';"
    public const val HSTS_ONE_YEAR_HEADER_VALUE: String = "max-age=31536000; includeSubDomains; preload"
    public const val NO_REFERRER_HEADER_VALUE: String = "no-referrer"
    public const val PERMISSION_POLICY_HEADER_VALUE: String =
        "camera=(), microphone=(), geolocation=(), " +
            "payment=(), usb=()"
}
