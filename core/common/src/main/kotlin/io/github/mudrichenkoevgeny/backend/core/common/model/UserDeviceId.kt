package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid

/**
 * Opaque identifier of a physical or logical user device.
 *
 * The format is intentionally unconstrained so that callers can plug in
 * platform-specific identifiers, push tokens or installation ids as needed.
 *
 * @param value Raw string backing the id; use [asHexDashString] for serialization.
 */
@JvmInline
value class UserDeviceId(val value: String) {

    /**
     * Returns the underlying value as a canonical string (e.g. for logging or API).
     */
    fun asHexDashString(): String = value

    companion object {

        /**
         * Generates a new random [UserDeviceId] (UUID hex-with-dashes form).
         */
        fun generate() = UserDeviceId(Uuid.random().toHexDashString())
    }
}

/**
 * Attempts to parse this string as a [UserDeviceId].
 *
 * Returns `null` if the string is blank.
 */
fun String.toUserDeviceIdOrNull(): UserDeviceId? =
    takeIf { it.isNotBlank() }?.let { UserDeviceId(it) }

/**
 * Parses this string into a [UserDeviceId] or throws if the string is blank.
 */
fun String.toUserDeviceIdOrThrow(): UserDeviceId {
    require(isNotBlank()) { "UserDeviceId cannot be blank" }
    return UserDeviceId(this)
}