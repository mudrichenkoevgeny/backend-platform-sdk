package com.mudrichenkoevgeny.backend.feature.user.mapper.user

import com.mudrichenkoevgeny.backend.feature.user.mapper.toResponse
import com.mudrichenkoevgeny.backend.feature.user.model.user.UserData
import com.mudrichenkoevgeny.backend.feature.user.network.response.user.UserResponse

fun UserData.toUserResponse(): UserResponse = this.user.toResponse(this.userIdentifiersList)