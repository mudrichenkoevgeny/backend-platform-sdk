package io.github.mudrichenkoevgeny.backend.feature.user.mapper

import io.github.mudrichenkoevgeny.backend.feature.user.model.auth.AuthData
import io.github.mudrichenkoevgeny.backend.feature.user.network.response.auth.AuthDataResponse

fun AuthData.toResponse(): AuthDataResponse = AuthDataResponse(
    userResponse = this.user.toResponse(this.userIdentifiersList),
    sessionTokenResponse = this.sessionToken.toResponse()
)