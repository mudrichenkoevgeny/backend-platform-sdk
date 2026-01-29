package io.github.mudrichenkoevgeny.backend.core.common.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

class ValidationException(val error: AppError) : RuntimeException()