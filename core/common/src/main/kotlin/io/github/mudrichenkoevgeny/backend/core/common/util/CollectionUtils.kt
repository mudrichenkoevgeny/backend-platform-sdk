package io.github.mudrichenkoevgeny.backend.core.common.util

fun isAllArgsNull(vararg values: Any?): Boolean = values.all { it == null }

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> {
    return mapTo(LinkedHashSet(), transform)
}