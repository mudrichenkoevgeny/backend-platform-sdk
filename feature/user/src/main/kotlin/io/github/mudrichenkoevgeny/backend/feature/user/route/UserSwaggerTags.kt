package io.github.mudrichenkoevgeny.backend.feature.user.route

/**
 * Swagger/OpenAPI tags used by the user feature routes.
 *
 * These tags are consumed by the OpenAPI/Swagger integration to group endpoints in UI.
 */
public object UserSwaggerTags {
    public const val AUTH: String = "Authentication"
    public const val SESSION: String = "Session"
    public const val USER: String = "User"
    public const val IDENTIFIER: String = "Identifiers"
    public const val AUTH_SETTINGS: String = "Auth settings"
    public const val USER_CONFIGURATION: String = "User configuration"
    public const val USER_SECURITY: String = "User security"
}
