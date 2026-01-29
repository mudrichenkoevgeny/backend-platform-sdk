package io.github.mudrichenkoevgeny.backend.core.common.validation

/**
 * Marks a field as required in the request payload.
 * The field must be present, but its content may be empty.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiredField

/**
 * Marks a String field as required and non-blank.
 * The field must be present and must not be empty or contain only whitespace.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotBlankStringField

/**
 * Marks a collection field as required and non-empty.
 * The field must be present and must contain at least one element.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class NotEmptyCollectionField