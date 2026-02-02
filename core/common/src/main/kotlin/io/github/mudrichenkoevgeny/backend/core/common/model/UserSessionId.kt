package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class UserSessionId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = UserSessionId(Uuid.random())
    }
}

@OptIn(ExperimentalUuidApi::class)
fun String.toUserSessionIdOrNull(): UserSessionId? =
    Uuid.parseOrNull(this)?.let { UserSessionId(it) }

@OptIn(ExperimentalUuidApi::class)
fun String.toUserSessionIdOrThrow(): UserSessionId = UserSessionId(Uuid.parse(this))