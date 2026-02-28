package io.github.mudrichenkoevgeny.backend.core.common.network.contract

object CommonNetworkHttpHeaderValues {
    const val SERVER_MASK_HEADER_VALUE = "Server"
    const val NOSNIFF_HEADER_VALUE = "nosniff"
    const val DENY_HEADER_VALUE = "DENY"
    const val XSS_BLOCK_HEADER_VALUE = "1; mode=block"
    const val CSP_API_HEADER_VALUE = "default-src 'self'; img-src 'self' https:; frame-ancestors 'none';"
    const val CSP_DEV_HEADER_VALUE = "default-src 'self' 'unsafe-inline'; connect-src 'self' http://localhost:* ws://localhost:*; img-src 'self' data: https:; frame-ancestors 'none';"
    const val HSTS_ONE_YEAR_HEADER_VALUE = "max-age=31536000; includeSubDomains; preload"
    const val NO_REFERRER_HEADER_VALUE = "no-referrer"
    const val PERMISSION_POLICY_HEADER_VALUE = "camera=(), microphone=(), geolocation=(), payment=(), usb=()"
}