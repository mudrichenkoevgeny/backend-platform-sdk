package io.github.mudrichenkoevgeny.backend.core.common.config.seed

import kotlinx.serialization.Serializable

@Serializable
data class AdminList(
    val admins: List<AdminAccount>
)