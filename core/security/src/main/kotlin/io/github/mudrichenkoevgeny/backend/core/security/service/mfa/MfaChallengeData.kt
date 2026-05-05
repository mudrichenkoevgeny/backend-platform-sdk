package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import kotlinx.serialization.Serializable

/**
 * Data container for an active MultiFactor Authentication challenge.
 * * This object is stored in temporary storage (e.g., Redis) and links a security
 * challenge token (mfaToken) to a specific user and context.
 *
 * @property token The unique, high-entropy string (mfaToken) identifying this challenge.
 * @property userId The unique identifier of the user who must fulfill the challenge.
 * Linked as [String] to maintain module decoupling.
 * @property userRole The role of the user at the time the challenge was created.
 * Used for audit logging and to maintain context throughout the multistep
 * authentication process without additional database lookups.
 * @property identifierId The identifier used during the first stage of authentication.
 * Used to link the MFA challenge to a specific login method
 * to ensure consistency when establishing the final session.
 * @property sessionId The identifier of the session that initiated the challenge.
 * Required for "Step-up" or "Re-authentication" flows to prevent session hijacking.
 * Is `null` during login flows where a session hasn't been established yet.
 * @property type The specific purpose of the challenge (e.g., LOGIN_TOTP, SETUP_TOTP).
 * @property metadata Additional key-value pairs required for specific flows,
 * such as a temporary TOTP secret during the setup phase.
 */
@Serializable
data class MfaChallengeData(
    val token: String,
    val userId: String,
    val userRole: String,
    val identifierId: String? = null,
    val sessionId: String? = null,
    val type: MfaChallengeType,
    val metadata: Map<String, String> = emptyMap()
)