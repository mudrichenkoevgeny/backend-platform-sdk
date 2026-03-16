package io.github.mudrichenkoevgeny.backend.core.common.model

/**
 * Opaque identifier of a physical or logical user device.
 *
 * The format is intentionally unconstrained so that callers can plug in
 * platform-specific identifiers, push tokens or installation ids as needed.
 */
@JvmInline
value class UserDeviceId(val value: String)