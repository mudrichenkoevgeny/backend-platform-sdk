package io.github.mudrichenkoevgeny.backend.feature.user.service.email.parser.model

/**
 * Configuration for classpath-based email template loading.
 *
 * For each [supportedLocales] entry and each [resourcePaths] entry, the parser attempts to read:
 * `"{path}/{locale}/{resourceFileName}.{resourceFileExtension}"`.
 *
 * Files from later paths in [resourcePaths] may override keys loaded from earlier paths.
 *
 * @property resourceFileName Base name of the resource file (e.g. `email_messages`).
 * @property resourceFileExtension Resource file extension (e.g. `json`).
 * @property resourcePaths Classpath directories to scan.
 * @property supportedLocales Locales to preload into the in-memory cache.
 */
data class EmailParserConfig(
    val resourceFileName: String = "email_messages",
    val resourceFileExtension: String = "json",
    val resourcePaths: List<String> = listOf("localization"),
    val supportedLocales: Set<String> = setOf("en", "ru")
)