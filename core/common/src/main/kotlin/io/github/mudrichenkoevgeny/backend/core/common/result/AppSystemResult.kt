package io.github.mudrichenkoevgeny.backend.core.common.result

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError

/**
 * A specialization of [AppResult] for low‑level system operations.
 *
 * This wrapper is intended for use cases where only internal, non‑business
 * errors are expected, represented by [CommonError.Internal].
 */
sealed class AppSystemResult<out T> {

    /**
     * Signals that a system‑level operation completed successfully and produced [data].
     */
    data class Success<out T>(val data: T) : AppSystemResult<T>()

    /**
     * Signals that a system‑level operation failed with an internal error.
     */
    data class Error(val internalError: CommonError.Internal) : AppSystemResult<Nothing>()
}