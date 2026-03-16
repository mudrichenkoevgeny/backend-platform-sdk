package io.github.mudrichenkoevgeny.backend.core.common.config.env

/**
 * Reads a delimited environment variable and converts it into a list of non-empty, trimmed strings.
 *
 * @param key environment variable name.
 * @param delimiter separator used between values, defaults to a comma.
 * @return list of values or an empty list when the variable is missing or blank.
 */
fun EnvReader.getStringList(key: String, delimiter: String = ","): List<String> {
    return getByKeyOrNull(key)
        ?.split(delimiter)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: emptyList()
}