package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.Uuid

@JvmInline
value class UserIdentifierId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = UserIdentifierId(Uuid.random())
    }
}

fun String.toUserIdentifierIdOrThrow(): UserIdentifierId = UserIdentifierId(Uuid.parse(this))