package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid

/**
 * Identifier of a user-related external identifier (for example, email or phone).
 *
 * Separating this from [UserId] allows modeling multiple identifiers that can
 * be attached to the same logical user.
 */
@JvmInline
value class UserIdentifierId(val value: Uuid) {

    /**
     * Returns the underlying [Uuid] as a canonical hex string with dashes.
     */
    fun asHexDashString(): String = value.toHexDashString()

    companion object {

        /**
         * Generates a new random [UserIdentifierId].
         */
        fun generate() = UserIdentifierId(Uuid.random())
    }
}

/**
 * Attempts to parse this string as a [UserIdentifierId].
 *
 * Returns `null` if the value is not a valid UUID in hex-with-dashes form.
 */
fun String.toUserIdentifierIdOrNull(): UserIdentifierId? =
    Uuid.parseOrNull(this)?.let { UserIdentifierId(it) }

/**
 * Parses this string into a [UserIdentifierId] or throws if the value is not a valid UUID.
 */
fun String.toUserIdentifierIdOrThrow(): UserIdentifierId = UserIdentifierId(Uuid.parse(this))