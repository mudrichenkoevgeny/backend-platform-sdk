package io.github.mudrichenkoevgeny.backend.core.common.config.env

fun EnvReader.getStringList(key: String, delimiter: String = ","): List<String> {
    return getByKeyOrNull(key)
        ?.split(delimiter)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}