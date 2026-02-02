package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class UserId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = UserId(Uuid.random())
    }
}

@OptIn(ExperimentalUuidApi::class)
fun String.toUserIdOrNull(): UserId? =
    Uuid.parseOrNull(this)?.let { UserId(it) }

@OptIn(ExperimentalUuidApi::class)
fun String.toUserIdOrThrow(): UserId = UserId(Uuid.parse(this))