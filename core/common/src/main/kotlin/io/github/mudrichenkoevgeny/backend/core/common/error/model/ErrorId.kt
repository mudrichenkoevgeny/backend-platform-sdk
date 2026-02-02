package io.github.mudrichenkoevgeny.backend.core.common.error.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@JvmInline
value class ErrorId(val value: Uuid) {
    fun asHexDashString(): String = value.toHexDashString()

    companion object {
        fun generate() = ErrorId(Uuid.random())
    }
}