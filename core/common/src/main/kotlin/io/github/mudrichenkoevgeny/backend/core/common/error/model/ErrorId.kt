package io.github.mudrichenkoevgeny.backend.core.common.error.model

import java.util.UUID

@JvmInline
value class ErrorId(val value: UUID) {
    companion object {
        fun generate(): ErrorId = ErrorId(UUID.randomUUID())
    }
}