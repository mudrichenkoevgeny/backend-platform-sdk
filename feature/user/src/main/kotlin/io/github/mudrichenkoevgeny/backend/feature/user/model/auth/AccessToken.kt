package io.github.mudrichenkoevgeny.backend.feature.user.model.auth

/**
 * Opaque JWT or bearer token string issued for API access.
 *
 * Used to carry the access token in [SessionToken] and in auth responses without
 * exposing raw strings at call sites.
 *
 * @param value Raw token string (e.g. JWT); must not be logged or serialized to clients in full.
 */
@JvmInline
value class AccessToken(val value: String)