package io.github.mudrichenkoevgeny.backend.core.common.network.contract

/**
 * Common HTTP header names used for security and proxy-related configuration.
 */
public object CommonNetworkHttpHeaders {
    public const val X_PREFIX: String = "x-"

    public const val X_FORWARDED_FOR: String = "X-Forwarded-For"
    public const val X_CONTENT_TYPE_OPTIONS_HEADER_NAME: String = "X-Content-Type-Options"
    public const val X_FRAME_OPTIONS_HEADER_NAME: String = "X-Frame-Options"
    public const val X_XSS_PROTECTION_HEADER_NAME: String = "X-XSS-Protection"
    public const val CONTENT_SECURITY_POLICY_HEADER_NAME: String = "Content-Security-Policy"
    public const val STRICT_TRANSPORT_SECURITY_HEADER_NAME: String = "Strict-Transport-Security"
    public const val REFERRER_POLICY_HEADER_NAME: String = "Referrer-Policy"
    public const val PERMISSION_POLICY_HEADER_NAME: String = "Permissions-Policy"
}
