package io.github.mudrichenkoevgeny.backend.core.common.model

sealed class UpdateField<out T> {
    object Ignore : UpdateField<Nothing>()
    data class Set<T>(val value: T?) : UpdateField<T>()
}

fun <T> UpdateField<T>.onSet(block: (T?) -> Unit) {
    if (this is UpdateField.Set) block(value)
}