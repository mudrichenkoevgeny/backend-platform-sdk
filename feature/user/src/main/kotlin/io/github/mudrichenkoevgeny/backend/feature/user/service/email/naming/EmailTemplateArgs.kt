package io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser

/**
 * Placeholder names used for templated email rendering.
 *
 * Placeholders are referenced in template text as `{key}` and replaced by [EmailParser].
 */
public object EmailTemplateArgs {
    /** One-time code used in verification/reset flows. */
    public const val CODE: String = "code"

    /** Optional client IP address for security notifications. */
    public const val IP_ADDRESS: String = "ipAddress"

    /** Optional client device name for security notifications. */
    public const val DEVICE_NAME: String = "deviceName"

    /** Value to replace the `{userAgent}` placeholder. */
    public const val USER_AGENT: String = "userAgent"
}
