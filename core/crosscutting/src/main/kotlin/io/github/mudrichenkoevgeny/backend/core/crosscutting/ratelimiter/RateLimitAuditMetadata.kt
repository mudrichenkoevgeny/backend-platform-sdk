package io.github.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

/**
 * Audit metadata keys and values used when a request is denied due to rate limiting.
 *
 * The metadata is intended for audit trails and operational investigations and is attached to
 * an audit event created by [RateLimitEnforcerImpl].
 */
object RateLimitAuditMetadata {
    /**
     * Metadata keys written into the audit event.
     */
    object Keys {
        const val IP_ADDRESS = "ip_address"
        const val DEVICE_ID = "device_id"
        const val CLIENT_TYPE = "client_type"
        const val USER_AGENT = "user_agent"

        const val REASON = "reason"
    }

    /**
     * Predefined metadata values for [Keys.REASON].
     */
    object Reasons {
        const val RATE_LIMIT = "rate_limit"
    }
}