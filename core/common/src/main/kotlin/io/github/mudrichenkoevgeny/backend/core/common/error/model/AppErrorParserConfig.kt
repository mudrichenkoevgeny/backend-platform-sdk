package io.github.mudrichenkoevgeny.backend.core.common.error.model

/**
 * @param resourceFileNamePrefix Prefix of the resource file names, e.g., "error_messages_".
 *                       Defaults to "error_messages_".
 *                       The full file name will be constructed as: {prefix}{locale}.{fileExtension}.
 * @param resourceFileExtension Extension of the resource files, e.g., "json". Defaults to "json".
 * @param resourcePaths List of classpath directories to search for resource files.
 *                      Files from later paths in the list can override earlier ones.
 * @param supportedLocales Set of locale strings supported by the application, e.g., {"en", "ru"}.
 */
data class AppErrorParserConfig(
    val resourceFileNamePrefix: String = "error_messages_",
    val resourceFileExtension: String = "json",
    val resourcePaths: List<String> = listOf("app_errors"),
    val supportedLocales: Set<String> = setOf("en")
)