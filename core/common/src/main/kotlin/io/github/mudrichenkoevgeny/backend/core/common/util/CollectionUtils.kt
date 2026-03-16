package io.github.mudrichenkoevgeny.backend.core.common.util

/**
 * Utility helpers for working with collections and vararg argument lists.
 */
object CollectionUtils {

    /**
     * Returns `true` if **all** provided [values] are `null`, `false` otherwise.
     */
    fun isAllArgsNull(vararg values: Any?): Boolean = values.all { it == null }
}