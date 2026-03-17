package io.github.mudrichenkoevgeny.backend.feature.user.mapper.model

import io.github.mudrichenkoevgeny.backend.feature.user.mapper.auth.toSessionTokenResponse
import io.github.mudrichenkoevgeny.backend.feature.user.mapper.user.toCurrentUserResponse
import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.response.auth.AuthDataResponse

/**
 * Maps internal [AuthData] to the shared network response contract.
 */
fun AuthData.toAuthDataResponse(): AuthDataResponse = AuthDataResponse(
    currentUserResponse = this.currentUser.toCurrentUserResponse(),
    sessionTokenResponse = this.sessionToken.toSessionTokenResponse()
)