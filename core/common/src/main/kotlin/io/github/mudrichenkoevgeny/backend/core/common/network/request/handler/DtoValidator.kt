package io.github.mudrichenkoevgeny.backend.core.common.network.request.handler

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotBlankStringField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.NotEmptyCollectionField
import io.github.mudrichenkoevgeny.shared.foundation.core.common.validation.RequiredField
import kotlinx.serialization.SerialName
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Validates an object's properties using reflection and custom annotations.
 * - [RequiredField]: Ensures the property value is not null.
 * - [NotBlankStringField]: Ensures the value is a [String] and is not blank.
 * - [NotEmptyCollectionField]: Ensures the value is a [Collection] and is not empty.
 *
 * @throws RequestHandlingException if any validation rule is violated, containing the corresponding [CommonError].
 */
fun Any.validateDto() {
    val kClass = this::class

    for (property in kClass.memberProperties) {
        val value = property.getter.call(this)
        val fieldName = property.findAnnotation<SerialName>()?.value ?: property.name

        property.findAnnotation<RequiredField>()?.let {
            if (value == null) {
                throw RequestHandlingException(CommonError.MissingRequiredField(fieldName))
            }
        }

        property.findAnnotation<NotBlankStringField>()?.let {
            if (value !is String || value.isBlank()) {
                throw RequestHandlingException(CommonError.BlankStringField(fieldName))
            }
        }

        property.findAnnotation<NotEmptyCollectionField>()?.let {
            if (value !is Collection<*> || value.isEmpty()) {
                throw RequestHandlingException(CommonError.EmptyCollectionField(fieldName))
            }
        }
    }
}