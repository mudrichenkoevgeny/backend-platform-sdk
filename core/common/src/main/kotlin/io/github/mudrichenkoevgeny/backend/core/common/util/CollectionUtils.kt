package io.github.mudrichenkoevgeny.backend.core.common.util

object CollectionUtils {
    fun isAllArgsNull(vararg values: Any?): Boolean = values.all { it == null }
}