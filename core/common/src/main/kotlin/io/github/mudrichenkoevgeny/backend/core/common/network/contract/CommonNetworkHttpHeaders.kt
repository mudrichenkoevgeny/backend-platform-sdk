package io.github.mudrichenkoevgeny.backend.core.common.network.contract

object CommonNetworkHttpHeaders {
    const val X_PREFIX = "x-"

    const val X_FORWARDED_FOR = "X-Forwarded-For"
    const val X_CONTENT_TYPE_OPTIONS_HEADER_NAME = "X-Content-Type-Options"
    const val X_FRAME_OPTIONS_HEADER_NAME = "X-Frame-Options"
    const val X_XSS_PROTECTION_HEADER_NAME = "X-XSS-Protection"
    const val CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy"
    const val STRICT_TRANSPORT_SECURITY_HEADER_NAME = "Strict-Transport-Security"
    const val REFERRER_POLICY_HEADER_NAME = "Referrer-Policy"
    const val PERMISSION_POLICY_HEADER_NAME = "Permissions-Policy"
}