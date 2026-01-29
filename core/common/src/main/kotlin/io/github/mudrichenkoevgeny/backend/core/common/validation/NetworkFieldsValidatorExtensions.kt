package io.github.mudrichenkoevgeny.backend.core.common.validation

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import kotlinx.serialization.SerialName
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Validates an object based on custom validation annotations:
 * - @RequiredField: must not be null
 * - @NotBlankStringField: must be non-null, non-blank String
 * - @NotEmptyCollectionField: must be non-null, non-empty collection
 *
 * Returns Result.Success(Unit) if all validations pass, otherwise Result.Error with field info.
 */
suspend inline fun <reified T : Any> ApplicationCall.validateRequest(): T {
    val request = receive<T>()
    val kClass = request::class

    for (property in kClass.memberProperties) {
        val value = property.getter.call(request)
        val fieldName = property.findAnnotation<SerialName>()?.value ?: property.name

        property.findAnnotation<RequiredField>()?.let {
            if (value == null) {
                throw ValidationException(CommonError.MissingRequiredField(fieldName))
            }
        }

        property.findAnnotation<NotBlankStringField>()?.let {
            if (value !is String || value.isBlank()) {
                throw ValidationException(CommonError.BlankStringField(fieldName))
            }
        }

        property.findAnnotation<NotEmptyCollectionField>()?.let {
            if (value !is Collection<*> || value.isEmpty()) {
                throw ValidationException(CommonError.EmptyCollectionField(fieldName))
            }
        }
    }

    return request
}