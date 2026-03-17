package io.github.mudrichenkoevgeny.backend.feature.user.service.email.naming

import io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.EmailParser
/**
 * Placeholder names used for templated email rendering.
 *
 * Placeholders are referenced in template text as `{key}` and replaced by [EmailParser].
 */
object EmailTemplateArgs {
    /** One-time code used in verification/reset flows. */
    const val CODE = "code"
    /** Optional client IP address for security notifications. */
    const val IP_ADDRESS = "ipAddress"
    /** Optional client device name for security notifications. */
    const val DEVICE_NAME = "deviceName"
}