package io.github.mudrichenkoevgeny.backend.core.common.model

import java.util.UUID

@JvmInline
value class UserSessionId(val value: UUID) {
    fun asString(): String = value.toString()
}

fun String.toUserSessionIdOrNull(): UserSessionId? =
    try {
        UserSessionId(UUID.fromString(this))
    } catch (_: IllegalArgumentException) {
        null
    }

fun String.toUserSessionIdOrThrow(): UserSessionId = UserSessionId(UUID.fromString(this))