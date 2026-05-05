package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.domain.model.user.createTestUserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.mockk.coEvery

fun AuthenticationProvider.mockRequireUserSuccess(user: UserDetails = createTestUserDetails()) {
    coEvery {
        requireUser(any(), any(), any(), any())
    } returns AppResult.Success(user)
}

fun AuthenticationProvider.mockRequireUserForbidden() {
    coEvery {
        requireUser(any(), any(), any(), any())
    } returns AppResult.Error(UserError.UserForbidden())
}