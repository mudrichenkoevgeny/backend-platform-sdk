package io.github.mudrichenkoevgeny.backend.core.common.error.model

import kotlin.uuid.Uuid

/**
 * Stable, unique identifier for a single error instance.
 *
 * Used in [AppError] and in [ApiErrorResponse][io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse]
 * so that the same id can be correlated in logs and client reports.
 *
 * @param value UUID backing the id; use [asHexDashString] for serialization (e.g. in JSON).
 */
@JvmInline
value class ErrorId(val value: Uuid) {

    /**
     * Returns the id as a canonical hex string (e.g. for API response or logging).
     */
    fun asHexDashString(): String = value.toHexDashString()

    companion object {

        /**
         * Creates a new unique [ErrorId] (random UUID).
         */
        fun generate() = ErrorId(Uuid.random())
    }
}