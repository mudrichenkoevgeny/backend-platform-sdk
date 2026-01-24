package com.mudrichenkoevgeny.backend.core.common.model

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    fun asString(): String = value.toString()
}

fun String.toUserIdOrNull(): UserId? =
    try {
        UserId(UUID.fromString(this))
    } catch (_: IllegalArgumentException) {
        null
    }

fun String.toUserIdOrThrow(): UserId = UserId(UUID.fromString(this))