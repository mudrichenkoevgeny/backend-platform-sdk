package io.github.mudrichenkoevgeny.backend.core.common.model

import java.util.UUID

@JvmInline
value class UserIdentifierId(val value: UUID) {
    fun asString(): String = value.toString()
}

fun String.toUserIdentifierIdOrThrow(): UserIdentifierId = UserIdentifierId(UUID.fromString(this))