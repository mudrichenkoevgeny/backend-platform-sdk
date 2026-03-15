package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid

@JvmInline
value class UserSessionId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = UserSessionId(Uuid.random())
    }
}

fun String.toUserSessionIdOrNull(): UserSessionId? =
    Uuid.parseOrNull(this)?.let { UserSessionId(it) }

fun String.toUserSessionIdOrThrow(): UserSessionId = UserSessionId(Uuid.parse(this))