package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

/**
 * @param resourceFileName Name of the resource file, e.g., "email_messages".
 * The full file name will be constructed as: {path}/{locale}/{resourceFileName}.{resourceFileExtension}.
 * @param resourceFileExtension Extension of the resource files, e.g., "json". Defaults to "json".
 * @param resourcePaths List of classpath directories to search for resource files.
 * Files from later paths in the list can override earlier ones.
 * @param supportedLocales Set of locale strings supported by the application, e.g., {"en", "ru"}.
 */
data class EmailParserConfig(
    val resourceFileName: String = "email_messages",
    val resourceFileExtension: String = "json",
    val resourcePaths: List<String> = listOf("localization"),
    val supportedLocales: Set<String> = setOf("en", "ru")
)