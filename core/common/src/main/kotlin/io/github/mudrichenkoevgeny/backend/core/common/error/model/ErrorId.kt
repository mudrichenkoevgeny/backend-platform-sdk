package io.github.mudrichenkoevgeny.backend.core.common.error.model

import kotlin.uuid.Uuid

@JvmInline
value class ErrorId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = ErrorId(Uuid.random())
    }
}