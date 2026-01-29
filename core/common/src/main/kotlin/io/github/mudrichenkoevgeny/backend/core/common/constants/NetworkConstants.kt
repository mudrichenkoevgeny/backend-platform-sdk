package io.github.mudrichenkoevgeny.backend.core.common.constants

object NetworkConstants {
    const val TRACE_HEADER_NAME = "X-Trace-Id"
    const val CLIENT_TYPE_HEADER_NAME = "X-Client-Type"
    const val DEVICE_ID_HEADER_NAME = "X-Device-Id"
    const val DEVICE_NAME_HEADER_NAME = "X-Device-Name"
    const val X_FORWARDED_FOR = "X-Forwarded-For"
    const val X_CONTENT_TYPE_OPTIONS_HEADER_NAME = "X-Content-Type-Options"
    const val X_FRAME_OPTIONS_HEADER_NAME = "X-Frame-Options"
    const val X_XSS_PROTECTION_HEADER_NAME = "X-XSS-Protection"
    const val CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy"
    const val STRICT_TRANSPORT_SECURITY_HEADER_NAME = "Strict-Transport-Security"
    const val REFERRER_POLICY_HEADER_NAME = "Referrer-Policy"
    const val PERMISSION_POLICY_HEADER_NAME = "Permissions-Policy"

    const val SERVER_MASK_HEADER_VALUE = "Server"
    const val NOSNIFF_HEADER_VALUE = "nosniff"
    const val DENY_HEADER_VALUE = "DENY"
    const val XSS_BLOCK_HEADER_VALUE = "1; mode=block"
    const val CSP_API_HEADER_VALUE = "default-src 'self'; img-src 'self' https:; frame-ancestors 'none';"
    const val HSTS_ONE_YEAR_HEADER_VALUE = "max-age=31536000; includeSubDomains; preload"
    const val NO_REFERRER_HEADER_VALUE = "no-referrer"
    const val PERMISSION_POLICY_HEADER_VALUE = "camera=(), microphone=(), geolocation=(), payment=(), usb=()"

    const val CORS_MAX_AGE_SECONDS = 3600L
}