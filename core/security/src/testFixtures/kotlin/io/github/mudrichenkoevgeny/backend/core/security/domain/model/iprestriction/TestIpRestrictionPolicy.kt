package io.github.mudrichenkoevgeny.backend.core.security.domain.model.iprestriction

import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.iprestriction.IpRestrictionPolicy

/**
 * Test factory for creating [IpRestrictionPolicy] instances in tests.
 */
fun createTestIpRestrictionPolicy(
    isBlacklistEnabled: Boolean = false,
    blacklist: List<String> = emptyList(),
    isWhitelistEnabled: Boolean = false,
    whitelist: List<String> = emptyList()
) = IpRestrictionPolicy(
    isBlacklistEnabled = isBlacklistEnabled,
    blacklist = blacklist,
    isWhitelistEnabled = isWhitelistEnabled,
    whitelist = whitelist
)
