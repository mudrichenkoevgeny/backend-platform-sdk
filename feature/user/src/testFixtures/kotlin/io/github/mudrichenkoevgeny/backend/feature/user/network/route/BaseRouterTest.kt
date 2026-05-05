package io.github.mudrichenkoevgeny.backend.feature.user.network.route

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.TestAuthenticationProvider
import io.github.mudrichenkoevgeny.shared.foundation.core.common.error.model.ApiErrorResponse
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach

abstract class BaseRouterTest {

    protected val authProvider = TestAuthenticationProvider()
    protected val appLogger = mockk<AppLogger>(relaxed = true)
    protected val appErrorParser = mockk<AppErrorParser>(relaxed = true)

    @BeforeEach
    fun baseSetUp() {
        clearMocks(appErrorParser, appLogger)

        every {
            appErrorParser.getApiErrorResponse(any<AppError>(), any<String>())
        } answers {
            val error = it.invocation.args[0] as AppError
            ApiErrorResponse(
                id = error.errorId.asHexDashString(),
                code = error.code,
                message = "Test error message",
                args = emptyMap()
            )
        }
    }
}