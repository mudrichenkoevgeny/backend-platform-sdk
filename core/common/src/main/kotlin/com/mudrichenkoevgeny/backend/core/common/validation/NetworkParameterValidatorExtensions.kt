package com.mudrichenkoevgeny.backend.core.common.validation

import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall

inline fun <reified T> ApplicationCall.validatePathParameter(
    name: String,
    mapper: (String) -> T
): T {
    val rawValue = parameters[name]
        ?: throw ValidationException(CommonError.MissingRequiredParameter(name))

    return try {
        mapper(rawValue)
    } catch (_: Exception) {
        throw ValidationException(CommonError.InvalidParameterValue(name))
    }
}