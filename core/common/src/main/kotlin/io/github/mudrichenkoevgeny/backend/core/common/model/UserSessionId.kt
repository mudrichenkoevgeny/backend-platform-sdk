package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid

/**
 * Identifier of an authenticated user session.
 *
 * Each login typically receives a dedicated [UserSessionId] so that sessions
 * can be revoked, audited and correlated in logs independently.
 */
@JvmInline
value class UserSessionId(val value: Uuid) {

    /**
     * Returns the underlying [Uuid] as a canonical hex string with dashes.
     */
    fun asHexDashString(): String = value.toHexDashString()

    companion object {

        /**
         * Generates a new random [UserSessionId].
         */
        fun generate() = UserSessionId(Uuid.random())
    }
}

/**
 * Attempts to parse this string as a [UserSessionId].
 *
 * Returns `null` if the value is not a valid UUID in hex-with-dashes form.
 */
fun String.toUserSessionIdOrNull(): UserSessionId? =
    Uuid.parseOrNull(this)?.let { UserSessionId(it) }

/**
 * Parses this string into a [UserSessionId] or throws if the value is not a valid UUID.
 */
fun String.toUserSessionIdOrThrow(): UserSessionId = UserSessionId(Uuid.parse(this))