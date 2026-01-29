package io.github.mudrichenkoevgeny.backend.core.common.config.seed

import kotlinx.serialization.Serializable

@Serializable
data class AdminAccount(
    val email: String,
    val password: String
)