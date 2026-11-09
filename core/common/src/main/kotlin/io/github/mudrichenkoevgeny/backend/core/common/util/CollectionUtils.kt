package io.github.mudrichenkoevgeny.backend.core.common.util

/**
 * Returns `true` if all passed arguments are `null`.
 */
public fun isAllArgsNull(vararg values: Any?): Boolean = values.all { it == null }

/**
 * Maps each element in this [Iterable] to a new destination [Set].
 */
public inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> = mapTo(
    LinkedHashSet(),
    transform
)
