package io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.TokenExpiredException
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.result.mapNotNullOrError
import io.github.mudrichenkoevgeny.backend.core.security.config.model.SecurityConfig
import io.github.mudrichenkoevgeny.backend.feature.user.config.model.UserConfig
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.user.UserRepository
import io.github.mudrichenkoevgeny.backend.feature.user.database.repository.usersession.UserSessionRepository
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getSessionIdFromCredential
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdFromCredential
import io.github.mudrichenkoevgeny.backend.feature.user.security.jwt.getUserIdFromPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserDetails
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserApiQueryParams
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.contract.UserAuthSpec
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JWT-based implementation of [AuthenticationProvider] for Ktor.
 *
 * Configures JWT auth, resolves user identity from token payload, and returns API errors in the auth
 * challenge through [AppErrorParser]. Authorization checks are applied in [requireUser].
 */
@Singleton
class JwtAuthenticationProvider @Inject constructor(
    private val securityConfig: SecurityConfig,
    private val userConfig: UserConfig,
    private val userRepository: UserRepository,
    private val userSessionRepository: UserSessionRepository,
    private val appErrorParser: AppErrorParser
) : AuthenticationProvider {

    override fun configureAuthentication(application: Application) {
        application.install(Authentication) {
            jwt(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
                realm = securityConfig.authRealm

                this.verifier(JWT.require(Algorithm.HMAC256(userConfig.jwtSecret)).build())

                authHeader { call ->
                    val authHeader = call.request.parseAuthorizationHeader()
                    if (authHeader != null) {
                        return@authHeader authHeader
                    }

                    val queryToken = call.request.queryParameters[UserApiQueryParams.ACCESS_TOKEN]
                    if (queryToken != null) {
                        return@authHeader HttpAuthHeader.Single(UserAuthSpec.TOKEN_TYPE_BEARER, queryToken)
                    }

                    null
                }

                validate { credential ->
                    try {
                        val userId = credential.getUserIdFromCredential()
                        val userResult = userRepository.getUserDetailsById(userId)
                        return@validate when (userResult) {
                            is AppResult.Success -> {
                                val sessionId = credential.getSessionIdFromCredential()
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

                    val apiError = appErrorParser.getApiErrorResponse(appError)
                    call.respond(appError.httpStatusCode, apiError)
                }
            }
        }
    }

    override suspend fun requireUser(
        call: ApplicationCall,
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>,
        requiredPermissions: Set<PermissionCode>
    ): AppResult<UserDetails> {
        val userIdResult = call.getUserIdFromPayload()
        val userId = when (userIdResult) {
            is AppResult.Success -> {
                userIdResult.data
            }
            is AppResult.Error -> {
                return userIdResult
            }
        }

        val userResult = userRepository.getUserDetailsById(
            userId = userId
        ).mapNotNullOrError(UserError.UserNotFound(userId))

        val user = when (userResult) {
            is AppResult.Success -> userResult.data
            is AppResult.Error -> return AppResult.Error(UserError.UserNotFound(userId))
        }

        if (user.role !in allowedRoles) {
            return AppResult.Error(UserError.UserRoleNotAllowed(userId))
        }

        if (user.accountStatus !in allowedAccountStatuses) {
            return AppResult.Error(
                when (user.accountStatus) {
                    UserAccountStatus.ACTIVE -> UserError.UserForbidden(userId)
                    UserAccountStatus.READ_ONLY -> UserError.UserReadOnly(userId)
                    UserAccountStatus.BANNED -> UserError.UserBlocked(userId)
                    UserAccountStatus.SECURITY_HOLD -> UserError.UserSecurityHold(userId)
                    UserAccountStatus.PENDING_DELETION -> UserError.UserPendingDeletion(userId)
                }
            )
        }

        if (requiredPermissions.isNotEmpty() && !user.permissionCodes.containsAll(requiredPermissions)) {
            return AppResult.Error(UserError.UserMissingPermissions(userId))
        }

        return AppResult.Success(user)
    }
}