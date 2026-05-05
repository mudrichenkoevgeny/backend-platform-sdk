package io.github.mudrichenkoevgeny.backend.core.security.service.mfa

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult

/**
 * Service for managing MultiFactor Authentication (MFA) challenges.
 *
 * It generates temporary tokens (mfaToken) that track the state of a multistep
 * authentication or sensitive operation (e.g., TOTP verification, password reset).
 */
interface MfaService {

    /**
     * Creates a new MFA challenge and stores it in the persistent storage.
     *
     * @param userId The unique identifier of the user.
     * @param userRole The role assigned to the user.
     * @param type The purpose of the challenge (e.g., LOGIN_TOTP).
     * @param identifierId The ID of the identifier used during the first stage of authentication.
     * @param sessionId The current session ID. Must be provided for non-login flows.
     * @param metadata Optional additional context data.
     * @return The created [MfaChallengeData] containing the generated token.
     */
    suspend fun createChallenge(
        userId: String,
        userRole: String,
        type: MfaChallengeType,
        identifierId: String? = null,
        sessionId: String? = null,
        metadata: Map<String, String>? = null
    ): AppResult<MfaChallengeData>

    /**
     * Retrieves the challenge data associated with the provided token string.
     *
     * @param token The mfaToken provided by the client.
     * @param type Expected type to prevent cross-usage between different flows.
     * @return The [MfaChallengeData] if valid, or an error if expired or type mismatch.
     */
    suspend fun getChallenge(
        token: String,
        type: MfaChallengeType
    ): AppResult<MfaChallengeData>

    /**
     * Consumes (invalidates) the challenge token after successful verification.
     * Should be called to prevent token reuse.
     *
     * @param token The token to delete.
     */
    suspend fun consumeChallenge(token: String): AppResult<Unit>

    /**
     * Retrieves, validates, and consumes (deletes) the challenge token in a single operation.
     *
     * Ensures that:
     * 1. The token exists and has not expired.
     * 2. The token belongs to the specified [userId].
     * 3. The token matches the expected [type].
     * 4. The token is bound to the provided [sessionId] (if applicable).
     *
     * @param token The mfaToken provided by the client.
     * @param type Expected type to prevent cross-usage between flows.
     * @param userId The unique identifier of the user who must own the token.
     * @param sessionId Optional session identifier. If the challenge was created with a sessionId,
     * this must match exactly.
     * @return [AppResult.Success] if all validations pass, otherwise [AppResult.Error].
     */
    suspend fun validateChallenge(
        token: String,
        type: MfaChallengeType,
        userId: String,
        sessionId: String? = null
    ): AppResult<Unit>
}