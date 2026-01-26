package com.mudrichenkoevgeny.backend.feature.user.audit

object UserAuditMetadata {
    object Keys {
        const val IP_ADDRESS = "ip_address"
        const val DEVICE_ID = "device_id"
        const val DEVICE_NAME = "device_name"
        const val CLIENT_TYPE = "client_type"
        const val USER_AGENT = "user_agent"
        const val EMAIL_MASK = "email_mask"
        const val PHONE_NUMBER_MASK = "phone_number_mask"
        const val EXTERNAL_AUTH_PROVIDER_TOKEN_MASK = "external_auth_provider_token_mask"
        const val EXTERNAL_AUTH_PROVIDER_TOKEN = "external_auth_provider_token"
        const val EXTERNAL_AUTH_PROVIDER = "external_auth_provider"
        const val SESSION_ID = "session_id"
        const val USER_ID = "user_id"
        const val USER_IDENTIFIER_ID = "user_identifier_id"
        const val AUTH_PROVIDER_KEY = "auth_provider_key"

        const val REASON = "reason"
        const val TYPE = "type"
    }

    object Reasons {

    }

    object Types {
        const val INTERNAL_ERROR = "internal_error"

        const val ALREADY_REGISTERED = "already_registered"
        const val VERIFICATION_CODE_SENT = "verification_code_sent"
        const val EMAIL_NOT_REGISTERED = "email_not_registered"
        const val WRONG_VERIFICATION_CODE = "wrong_verification_code"
        const val WRONG_PASSWORD = "wrong_password"
        const val TOO_WEAK_PASSWORD = "too_weak_password"
        const val INVALID_REFRESH_TOKEN = "invalid_refresh_token"
        const val NOT_SUPPORTED_EXTERNAL_AUTH_PROVIDER = "not_supported_external_auth_provider"
        const val CAN_NOT_DELETE_USER_IDENTIFIER = "can_not_delete_user_identifier"
        const val ALREADY_HAS_USER_IDENTIFIER_WITH_THAT_TYPE = "already_has_user_identifier_with_that_type"
        const val AUTHENTICATION_CONFIRMATION_REQUIRED = "authentication_confirmation_required"
        const val CAN_NOT_DELETE_USER = "can_not_delete_user"
        const val CAN_NOT_GET_USER = "can_not_get_user"
        const val EMAIL_ALREADY_REGISTERED = "email_already_registered"
        const val PHONE_ALREADY_REGISTERED = "phone_already_registered"
    }
}