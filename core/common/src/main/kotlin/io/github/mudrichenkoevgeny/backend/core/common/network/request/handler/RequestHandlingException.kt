package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * Thrown when request handling fails with an application-level error.
 *
 * Wraps an [AppError] that can later be logged or mapped to an HTTP response.
 */
class RequestHandlingException(val error: AppError) : RuntimeException()