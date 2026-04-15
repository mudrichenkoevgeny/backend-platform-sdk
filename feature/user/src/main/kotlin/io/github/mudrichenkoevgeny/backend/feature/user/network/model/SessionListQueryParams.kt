package io.github.mudrichenkoevgeny.backend.feature.user.network.model

import io.github.mudrichenkoevgeny.backend.core.common.pagination.ListingQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.client.ClientType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.authprovider.UserAuthProvider
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.identifier.UserIdentifierId
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.listing.UserSortValues
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId

/**
 * Query parameters for management user sessions list endpoint.
 */
data class SessionListQueryParams(
    val listing: ListingQueryParams<UserSortValues.UserSessionSortBy>,
    val userIds: List<UserId>,
    val identifiers: List<String>,
    val identifierIds: List<UserIdentifierId>,
    val identifierAuthProviders: List<UserAuthProvider>,
    val revokedValues: List<Boolean>,
    val clientTypes: List<ClientType>,
    val userAgents: List<String>,
    val ipAddresses: List<String>,
    val languages: List<String>,
    val deviceIds: List<String>,
    val deviceNames: List<String>,
    val appVersions: List<String>,
    val operationSystemVersions: List<String>
)
