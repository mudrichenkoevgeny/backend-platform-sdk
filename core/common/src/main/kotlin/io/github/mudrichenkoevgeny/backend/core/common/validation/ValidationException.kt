package io.github.mudrichenkoevgeny.backend.core.common.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * Thrown when a validation rule for a DTO, request body, or parameter fails.
 *
 * Wraps a domain‑specific [AppError] that can later be logged or mapped to an HTTP response.
 */
class ValidationException(val error: AppError) : RuntimeException()