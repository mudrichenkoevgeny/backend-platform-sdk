package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid

@JvmInline
value class UserId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = UserId(Uuid.random())
    }
}

fun String.toUserIdOrNull(): UserId? =
    Uuid.parseOrNull(this)?.let { UserId(it) }

fun String.toUserIdOrThrow(): UserId = UserId(Uuid.parse(this))