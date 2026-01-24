package com.mudrichenkoevgeny.backend.core.crosscutting.ratelimiter

object RateLimitAuditMetadata {
    object Keys {
        const val IP_ADDRESS = "ip_address"
        const val DEVICE_ID = "device_id"
        const val CLIENT_TYPE = "client_type"
        const val USER_AGENT = "user_agent"

        const val REASON = "reason"
    }

    object Reasons {
        const val RATE_LIMIT = "rate_limit"
    }
}