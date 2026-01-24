package com.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.TokenExpiredException
import com.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import com.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import com.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import com.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import com.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import com.mudrichenkoevgeny.backend.feature.user.enums.UserAccountStatus
import com.mudrichenkoevgeny.backend.feature.user.enums.UserRole
import com.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import com.mudrichenkoevgeny.backend.feature.user.model.user.User
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionId
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserId
import com.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdFromPayload
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtAuthenticationProvider @Inject constructor(
    private val userConfig: UserConfig,
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val appErrorParser: AppErrorParser
) : AuthenticationProvider {

    override fun configureAuthentication(application: Application) {
        application.install(Authentication) {
            jwt(JwtAuthenticationConstants.AUTHENTICATE_CONFIGURATION) {
                realm = userConfig.authRealm

                this.verifier(JWT.require(Algorithm.HMAC256(userConfig.jwtSecret)).build())

                validate { credential ->
                    try {
                        val userId = credential.getUserId()
                        val userResult = userRepository.getUserById(userId)
                        return@validate when (userResult) {
                            is AppResult.Success -> {
                                val sessionId = credential.getSessionId()
                                if (sessionId != null) {
                                    userSessionRepository.updateLastAccessed(sessionId)
                                }
                                JWTPrincipal(credential.payload)
                            }
                            is AppResult.Error -> JwtValidationError.UserNotFound
                        }
                    } catch (_: JWTDecodeException) {
                        JwtValidationError.InvalidToken
                    } catch (_: TokenExpiredException) {
                        JwtValidationError.TokenExpired
                    }
                }

                challenge { _, _ ->
                    val appError = (call.authentication.principal<JwtValidationError>())?.toAppError()
                        ?: UserError.InvalidAccessToken()

                    val apiError = appErrorParser.getApiError(appError)
                    call.respond(appError.httpStatusCode, apiError)
                }
            }
        }
    }

    override suspend fun requireUser(
        call: ApplicationCall,
        allowedRoles: Set<UserRole>,
        allowReadOnlyAccounts: Boolean,
        allowBannedAccounts: Boolean
    ): AppResult<User> {
        val userId = when (val userIdResult = call.getUserIdFromPayload()) {
            is AppResult.Success -> {
                userIdResult.data
            }
            is AppResult.Error -> {
                return userIdResult
            }
        }

        val userResult = userRepository.getUserById(
            userId = userId
        ).mapNotNullOrError(UserError.UserNotFound(userId))

        val user = when (userResult) {
            is AppResult.Success -> userResult.data
            is AppResult.Error -> return AppResult.Error(UserError.UserNotFound(userId))
        }

        if (user.role !in allowedRoles) {
            return AppResult.Error(UserError.UserForbidden(userId))
        }

        if (user.accountStatus == UserAccountStatus.BANNED && !allowBannedAccounts) {
            return AppResult.Error(UserError.UserBlocked(userId))
        }

        if (user.accountStatus == UserAccountStatus.READ_ONLY && !allowReadOnlyAccounts) {
            return AppResult.Error(UserError.UserReadOnly(userId))
        }

        return AppResult.Success(user)
    }
}