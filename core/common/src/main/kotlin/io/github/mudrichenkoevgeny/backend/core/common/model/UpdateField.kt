package io.github.mudrichenkoevgeny.backend.core.common.model

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.network.request.handler.RequestHandlingException
import io.github.mudrichenkoevgeny.shared.foundation.core.common.network.model.patch.PatchField

/**
 * Represents a tri-state update operation for domain models and database entities.
 *
 * Used across use cases and repositories to distinguish between omitting an update
 * and explicitly setting a field to a new value or `null`.
 *
 * @param T The type of the wrapped value.
 */
public sealed class UpdateField<out T> {
    /**
     * Indicates that the field should be ignored during update operations.
     */
    public data object Ignore : UpdateField<Nothing>()

    /**
     * Indicates that the field must be updated to the provided [value].
     *
     * @property value The target value to set, or `null` to clear the field.
     */
    public data class Set<T>(
        public val value: T?
    ) : UpdateField<T>()
}

/**
 * Executes [block] with the target value only when this [UpdateField] is [UpdateField.Set].
 *
 * @param T The type of the wrapped value.
 * @param block The lambda invoked with the nullable target value.
 */
public fun <T> UpdateField<T>.onSet(block: (T?) -> Unit) {
    if (this is UpdateField.Set) {
        block(value)
    }
}

/**
 * Converts a network-layer [PatchField] into a domain-layer [UpdateField].
 *
 * @param T The type of the wrapped value.
 * @return [UpdateField.Ignore] if unchanged, or [UpdateField.Set] wrapping the target value.
 */
public fun <T> PatchField<T>.toUpdateField(): UpdateField<T> = when (this) {
    is PatchField.Unchanged -> UpdateField.Ignore
    is PatchField.Value -> UpdateField.Set(value)
}

/**
 * Converts a network-layer [PatchField] into a domain-layer [UpdateField],
 * transforming the non-null value using [transform].
 *
 * @param T The source value type.
 * @param R The target domain value type.
 * @param transform Mapping function applied to non-null values.
 * @return [UpdateField.Ignore] if unchanged, or [UpdateField.Set] wrapping the transformed value or `null`.
 */
public fun <T, R> PatchField<T>.toUpdateField(transform: (T) -> R?): UpdateField<R> = when (this) {
    is PatchField.Unchanged -> UpdateField.Ignore
    is PatchField.Value -> UpdateField.Set(value?.let(transform))
}

/**
 * Maps a [PatchField] to [UpdateField] while strictly validating present non-null values.
 *
 * @throws RequestHandlingException if [parser] returns null for a present value.
 */
public fun <T, R> PatchField<T>.validateAndMap(
    fieldName: String,
    parser: (T) -> R?
): UpdateField<R> = when (this) {
    is PatchField.Unchanged -> UpdateField.Ignore
    is PatchField.Value -> {
        val rawValue = this.value
        if (rawValue == null) {
            UpdateField.Set(null)
        } else {
            val parsed = parser(rawValue) ?: throw RequestHandlingException(
                CommonError.InvalidParameterValue(fieldName)
            )
            UpdateField.Set(parsed)
        }
    }
}
