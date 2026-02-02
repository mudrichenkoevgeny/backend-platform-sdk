package io.github.mudrichenkoevgeny.backend.core.common.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class UserIdentifierId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = UserIdentifierId(Uuid.random())
    }
}

@OptIn(ExperimentalUuidApi::class)
fun String.toUserIdentifierIdOrThrow(): UserIdentifierId = UserIdentifierId(Uuid.parse(this))