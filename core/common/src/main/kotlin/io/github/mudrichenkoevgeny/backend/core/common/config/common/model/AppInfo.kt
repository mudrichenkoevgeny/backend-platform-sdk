package io.github.mudrichenkoevgeny.backend.core.common.config.common.model

/**
 * Provides essential application metadata to the SDK.
 *
 * This interface acts as a contract between the library and the consuming application.
 * The application must provide a concrete implementation (typically via Dagger)
 * to ensure the SDK can identify the service name and version for logging,
 * monitoring, and configuration purposes.
 *
 * ### Integration Example:
 * ```kotlin
 * @Singleton
 * class BuildConfigAppInfo @Inject constructor() : AppInfo {
 *     override val version: String = BuildConfig.VERSION
 *     override val appName: String = BuildConfig.APP_NAME
 * }
 * ```
 */
interface AppInfo {
    /**
     * The current version of the application (e.g., "1.2.0").
     * Usually synchronized with the Gradle project version.
     */
    val version: String

    /**
     * The unique name of the application (e.g., "backend-app").
     * Used for service identification in distributed systems.
     */
    val appName: String
}