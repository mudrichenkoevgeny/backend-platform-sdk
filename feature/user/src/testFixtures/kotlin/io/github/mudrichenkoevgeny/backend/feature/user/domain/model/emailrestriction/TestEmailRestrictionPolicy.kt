package io.github.mudrichenkoevgeny.backend.feature.user.domain.model.emailrestriction

import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.emailrestriction.EmailRestrictionPolicy

/**
 * Test factory for creating [EmailRestrictionPolicy] instances in tests.
 */
fun createTestEmailRestrictionPolicy(
    isBlacklistEnabled: Boolean = false,
    blacklist: List<String> = emptyList(),
    isWhitelistEnabled: Boolean = false,
    whitelist: List<String> = emptyList()
) = EmailRestrictionPolicy(
    isBlacklistEnabled = isBlacklistEnabled,
    blacklist = blacklist,
    isWhitelistEnabled = isWhitelistEnabled,
    whitelist = whitelist
)
