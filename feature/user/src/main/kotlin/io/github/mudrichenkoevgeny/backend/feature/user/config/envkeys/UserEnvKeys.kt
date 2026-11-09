package io.github.mudrichenkoevgeny.backend.feature.user.config.envkeys

/**
 * Environment variable names used to configure the user feature.
 *
 * Values are read by the default configuration factory (`UserConfigFactoryImpl`) using an env reader
 * and secret files where applicable.
 */
public object UserEnvKeys {
    public const val JWT_SECRET_FILE: String = "JWT_SECRET_FILE"
    public const val ACCESS_TOKEN_EXPIRATION_SECONDS: String = "ACCESS_TOKEN_EXPIRATION_SECONDS"
    public const val REFRESH_TOKEN_EXPIRATION_SECONDS: String = "REFRESH_TOKEN_EXPIRATION_SECONDS"
    public const val ADMIN_ACCOUNTS_JSON_SECRET_FILE: String = "ADMIN_ACCOUNTS_JSON_SECRET_FILE"
    public const val AVAILABLE_AUTH_PROVIDERS_PRIMARY: String = "AVAILABLE_AUTH_PROVIDERS_PRIMARY"
    public const val AVAILABLE_AUTH_PROVIDERS_SECONDARY: String = "AVAILABLE_AUTH_PROVIDERS_SECONDARY"
    public const val MAX_TOTAL_IDENTIFIERS: String = "MAX_TOTAL_IDENTIFIERS"
    public const val MAX_EMAIL_IDENTIFIERS: String = "MAX_EMAIL_IDENTIFIERS"
    public const val MAX_PHONE_IDENTIFIERS: String = "MAX_PHONE_IDENTIFIERS"
    public const val MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER: String = "MAX_IDENTIFIERS_PER_EXTERNAL_PROVIDER"
    public const val USER_NICKNAME_MIN_LENGTH: String = "USER_NICKNAME_MIN_LENGTH"
    public const val USER_NICKNAME_MAX_LENGTH: String = "USER_NICKNAME_MAX_LENGTH"
    public const val USER_DISPLAY_NAME_MIN_LENGTH: String = "USER_DISPLAY_NAME_MIN_LENGTH"
    public const val USER_DISPLAY_NAME_MAX_LENGTH: String = "USER_DISPLAY_NAME_MAX_LENGTH"
    public const val IS_USER_NICKNAME_CHANGE_ALLOWED: String = "IS_USER_NICKNAME_CHANGE_ALLOWED"
    public const val MAX_ACTIVE_SESSIONS_FOR_OPEN_USER: String = "MAX_ACTIVE_SESSIONS_FOR_OPEN_USER"
    public const val MAX_ACTIVE_SESSIONS_FOR_MANAGEMENT_USER: String = "MAX_ACTIVE_SESSIONS_FOR_MANAGEMENT_USER"
    public const val ACCOUNT_DELETION_GRACE_PERIOD_SECONDS: String = "ACCOUNT_DELETION_GRACE_PERIOD_SECONDS"
    public const val ACCOUNT_DELETION_CHECK_INTERVAL_SECONDS: String = "ACCOUNT_DELETION_CHECK_INTERVAL_SECONDS"
    public const val IS_REGISTRATION_ENABLED: String = "IS_REGISTRATION_ENABLED"
    public const val IS_EMAIL_BLACKLIST_ENABLED_OPEN: String = "IS_EMAIL_BLACKLIST_ENABLED_OPEN"
    public const val EMAIL_BLACKLIST_OPEN: String = "EMAIL_BLACKLIST_OPEN"
    public const val IS_EMAIL_WHITELIST_ENABLED_OPEN: String = "IS_EMAIL_WHITELIST_ENABLED_OPEN"
    public const val EMAIL_WHITELIST_OPEN: String = "EMAIL_WHITELIST_OPEN"
    public const val IS_EMAIL_BLACKLIST_ENABLED_MANAGEMENT: String = "IS_EMAIL_BLACKLIST_ENABLED_MANAGEMENT"
    public const val EMAIL_BLACKLIST_MANAGEMENT: String = "EMAIL_BLACKLIST_MANAGEMENT"
    public const val IS_EMAIL_WHITELIST_ENABLED_MANAGEMENT: String = "IS_EMAIL_WHITELIST_ENABLED_MANAGEMENT"
    public const val EMAIL_WHITELIST_MANAGEMENT: String = "EMAIL_WHITELIST_MANAGEMENT"
    public const val GOOGLE_WEB_CLIENT_ID: String = "GOOGLE_WEB_CLIENT_ID"
    public const val UNIONE_API_KEY_FILE: String = "UNIONE_API_KEY_FILE"
    public const val UNIONE_URL: String = "UNIONE_URL"
    public const val UNIONE_FROM_EMAIL: String = "UNIONE_FROM_EMAIL"
    public const val UNIONE_FROM_NAME: String = "UNIONE_FROM_NAME"
    public const val UNIONE_TRACK_DOMAIN: String = "UNIONE_TRACK_DOMAIN"
    public const val UNIONE_API_SEND: String = "UNIONE_API_SEND"
    public const val RESEND_API_KEY_FILE: String = "RESEND_API_KEY_FILE"
    public const val RESEND_URL: String = "RESEND_URL"
    public const val RESEND_FROM_EMAIL: String = "RESEND_FROM_EMAIL"
    public const val RESEND_FROM_NAME: String = "RESEND_FROM_NAME"
}
