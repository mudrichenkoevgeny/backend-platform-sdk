package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSessionSortBy
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import kotlin.time.Instant

/**
 * Query parameters for management user sessions list endpoint.
 */
public data class ManagementSessionListQueryParams(
    val listing: ListingQueryParams<UserSessionSortBy>,
    val sessionIds: List<String> = emptyList(),
    val userIds: List<String> = emptyList(),
    val userRoles: List<UserRole> = emptyList(),
    val excludeUserRoles: List<UserRole> = emptyList(),
    val identifier: String? = null,
    val identifierIds: List<String> = emptyList(),
    val identifierDisplayName: String? = null,
    val identifierAuthProviders: List<UserAuthProvider> = emptyList(),
    val excludeIdentifierAuthProviders: List<UserAuthProvider> = emptyList(),
    val clientTypes: List<ClientType> = emptyList(),
    val excludeClientTypes: List<ClientType> = emptyList(),
    val isClientTypeSet: Boolean? = null,
    val deviceId: String? = null,
    val isDeviceIdSet: Boolean? = null,
    val deviceName: String? = null,
    val isDeviceNameSet: Boolean? = null,
    val language: String? = null,
    val isLanguageSet: Boolean? = null,
    val appVersion: String? = null,
    val isAppVersionSet: Boolean? = null,
    val osVersion: String? = null,
    val isOperationSystemVersionSet: Boolean? = null,
    val userAgent: String? = null,
    val isUserAgentSet: Boolean? = null,
    val ipAddress: String? = null,
    val isIpAddressSet: Boolean? = null,
    val expiresAtFrom: Instant? = null,
    val expiresAtTo: Instant? = null,
    val lastAccessedAtFrom: Instant? = null,
    val lastAccessedAtTo: Instant? = null,
    val lastReauthenticatedAtFrom: Instant? = null,
    val lastReauthenticatedAtTo: Instant? = null,
    val createdAtFrom: Instant? = null,
    val createdAtTo: Instant? = null,
    val updatedAtFrom: Instant? = null,
    val updatedAtTo: Instant? = null,
    val isUpdatedAtSet: Boolean? = null,
    val searchQuery: String? = null
)
