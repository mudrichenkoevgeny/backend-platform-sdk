package io.github.mudrichenkoevgeny.backend.feature.user.mapper.user

import io.github.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import io.github.mudrichenkoevgeny.backend.feature.user.model.user.UserData
import io.github.mudrichenkoevgeny.backend.feature.user.network.response.user.UserResponse

fun UserData.toUserResponse(): UserResponse = this.user.toResponse(this.userIdentifiersList)