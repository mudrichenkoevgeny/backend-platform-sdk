package io.github.mudrichenkoevgeny.backend.feature.user.route.management.user.security

import io.github.mudrichenkoevgeny.backend.core.audit.error.AuditErrorConverter
import io.github.mudrichenkoevgeny.backend.core.audit.logger.AuditLogger
import io.github.mudrichenkoevgeny.backend.core.common.documentation.swagger.formatter.getFormattedDescription
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.error.parser.AppErrorParser
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.common.routing.BaseRouter
import io.github.mudrichenkoevgeny.backend.core.common.routing.respondResult
import io.github.mudrichenkoevgeny.backend.core.common.util.mapToSet
import io.github.mudrichenkoevgeny.backend.feature.user.network.request.AuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.network.utils.getAuthenticatedRequestContext
import io.github.mudrichenkoevgeny.backend.feature.user.route.UserSwaggerTags
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.AuthenticationProvider
import io.github.mudrichenkoevgeny.backend.feature.user.security.authenticationprovider.JwtAuthSpecs
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security.DisableTotpUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security.EnableTotpUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security.GetRecoveryCodesUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security.RegenerateRecoveryCodesUseCase
import io.github.mudrichenkoevgeny.backend.feature.user.usecase.open.user.security.SetupTotpUseCase
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.totprecoverycodes.toTotpRecoveryCodesPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.mapper.totpsetup.toTotpSetupPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.totprecoverycodes.TotpRecoveryCodesPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.totpsetup.TotpSetupPayload
import io.github.mudrichenkoevgeny.shared.foundation.core.security.network.model.verifytotp.VerifyTotpPayload
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.audit.resource.UserAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.accountstatus.UserAccountStatus
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.network.route.management.user.security.SelfManagementUserSecurityRoutes
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.plus

/**
 * Management HTTP routes for self-service user security and Multi-Factor Authentication (MFA).
 *
 * Registered routes:
 * 1. [SelfManagementUserSecurityRoutes.SETUP_TOTP] — initiates TOTP setup and generates secrets via [SetupTotpUseCase].
 * 2. [SelfManagementUserSecurityRoutes.ENABLE_TOTP] — finalizes and activates TOTP for the account via [EnableTotpUseCase].
 * 3. [SelfManagementUserSecurityRoutes.DISABLE_TOTP] — removes TOTP protection and invalidates recovery codes via [DisableTotpUseCase].
 * 4. [SelfManagementUserSecurityRoutes.GET_RECOVERY_CODES] — retrieves the current list of active recovery codes via [GetRecoveryCodesUseCase].
 * 5. [SelfManagementUserSecurityRoutes.REGENERATE_RECOVERY_CODES] — replaces existing recovery codes with a new set via [RegenerateRecoveryCodesUseCase].
 */
@Singleton
class SelfManagementUserSecurityRouter @Inject constructor(
    private val authenticationProvider: AuthenticationProvider,
    private val appLogger: AppLogger,
    private val appErrorParser: AppErrorParser,
    private val auditLogger: AuditLogger,
    private val auditErrorConverter: AuditErrorConverter,
    private val setupTotpUseCase: SetupTotpUseCase,
    private val enableTotpUseCase: EnableTotpUseCase,
    private val disableTotpUseCase: DisableTotpUseCase,
    private val getRecoveryCodesUseCase: GetRecoveryCodesUseCase,
    private val regenerateRecoveryCodesUseCase: RegenerateRecoveryCodesUseCase
) : BaseRouter {

    override fun register(route: Route) {
        route.authenticate(JwtAuthSpecs.AUTHENTICATE_CONFIGURATION) {
            registerSetupTotpRoute(this)
            registerEnableTotpRoute(this)
            registerDisableTotpRoute(this)
            registerGetRecoveryCodesRoute(this)
            registerRegenerateRecoveryCodesRoute(this)
        }
    }

    private fun registerSetupTotpRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = SelfManagementUserSecurityRoutes.SETUP_TOTP,
            builder = { setupTotpDocs(allowedRoles, allowedAccountStatuses) },
            body = { setupTotp(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerEnableTotpRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = SelfManagementUserSecurityRoutes.ENABLE_TOTP,
            builder = { enableTotpDocs(allowedRoles, allowedAccountStatuses) },
            body = { enableTotp(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerDisableTotpRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.delete(
            path = SelfManagementUserSecurityRoutes.DISABLE_TOTP,
            builder = { disableTotpDocs(allowedRoles, allowedAccountStatuses) },
            body = { disableTotp(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerGetRecoveryCodesRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.get(
            path = SelfManagementUserSecurityRoutes.GET_RECOVERY_CODES,
            builder = { getRecoveryCodesDocs(allowedRoles, allowedAccountStatuses) },
            body = { getRecoveryCodes(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun registerRegenerateRecoveryCodesRoute(route: Route) {
        val allowedRoles = setOf(UserRole.USER)
        val allowedAccountStatuses = setOf(UserAccountStatus.ACTIVE, UserAccountStatus.READ_ONLY)

        route.post(
            path = SelfManagementUserSecurityRoutes.REGENERATE_RECOVERY_CODES,
            builder = { regenerateRecoveryCodesDocs(allowedRoles, allowedAccountStatuses) },
            body = { regenerateRecoveryCodes(allowedRoles, allowedAccountStatuses) }
        )
    }

    private fun RouteConfig.setupTotpDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = SETUP_TOTP_SUMMARY
        operationId = SETUP_TOTP_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_SECURITY)
        description = getFormattedDescription(
            description = SETUP_TOTP_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )
        response {
            code(HttpStatusCode.OK) {
                description = SETUP_TOTP_RESPONSE_OK_DESCRIPTION
                body<TotpSetupPayload>()
            }
        }
    }

    private suspend fun RoutingContext.setupTotp(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = setupTotpUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        call.respondResult(result, appLogger, appErrorParser) { totpSetup ->
            totpSetup.toTotpSetupPayload()
        }
    }

    private fun RouteConfig.enableTotpDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = ENABLE_TOTP_SUMMARY
        operationId = ENABLE_TOTP_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_SECURITY)
        description = getFormattedDescription(
            description = ENABLE_TOTP_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )
        request { body<VerifyTotpPayload>() }
        response {
            code(HttpStatusCode.OK) {
                description = ENABLE_TOTP_RESPONSE_OK_DESCRIPTION
                body<TotpRecoveryCodesPayload>()
            }
        }
    }

    private suspend fun RoutingContext.enableTotp(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val payload = call.receive<VerifyTotpPayload>()

        val result = enableTotpUseCase(
            authenticatedRequestContext = authenticatedRequestContext,
            mfaToken = payload.mfaToken,
            code = payload.code
        )

        if (result is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.SELF_ENABLE_TOTP,
                error = result.error
            )
        }

        call.respondResult(result, appLogger, appErrorParser) { totpRecoveryCodes ->
            totpRecoveryCodes.toTotpRecoveryCodesPayload()
        }
    }

    private fun RouteConfig.disableTotpDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = DISABLE_TOTP_SUMMARY
        operationId = DISABLE_TOTP_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_SECURITY)
        description = getFormattedDescription(
            description = DISABLE_TOTP_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )
        response {
            code(HttpStatusCode.OK) {
                description = DISABLE_TOTP_RESPONSE_OK_DESCRIPTION
            }
        }
    }

    private suspend fun RoutingContext.disableTotp(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = disableTotpUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        if (result is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.SELF_DISABLE_TOTP,
                error = result.error
            )
        }

        call.respondResult(result, appLogger, appErrorParser)
    }

    private fun RouteConfig.getRecoveryCodesDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = GET_RECOVERY_CODES_SUMMARY
        operationId = GET_RECOVERY_CODES_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_SECURITY)
        description = getFormattedDescription(
            description = GET_RECOVERY_CODES_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )
        response {
            code(HttpStatusCode.OK) {
                description = GET_RECOVERY_CODES_RESPONSE_OK_DESCRIPTION
                body<TotpRecoveryCodesPayload>()
            }
        }
    }

    private suspend fun RoutingContext.getRecoveryCodes(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = getRecoveryCodesUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        if (result is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.SELF_GET_RECOVERY_CODES,
                error = result.error
            )
        }

        call.respondResult(result, appLogger, appErrorParser) { totpRecoveryCodes ->
            totpRecoveryCodes.toTotpRecoveryCodesPayload()
        }
    }

    private fun RouteConfig.regenerateRecoveryCodesDocs(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        summary = REGENERATE_RECOVERY_CODES_SUMMARY
        operationId = REGENERATE_RECOVERY_CODES_OPERATION_ID
        tags = listOf(UserSwaggerTags.USER_SECURITY)
        description = getFormattedDescription(
            description = REGENERATE_RECOVERY_CODES_DESCRIPTION,
            allowedRoles = allowedRoles.mapToSet { it.serialName },
            allowedAccountStatuses = allowedAccountStatuses.mapToSet { it.serialName },
            isPublic = false
        )
        response {
            code(HttpStatusCode.OK) {
                description = REGENERATE_RECOVERY_CODES_RESPONSE_OK_DESCRIPTION
                body<TotpRecoveryCodesPayload>()
            }
        }
    }

    private suspend fun RoutingContext.regenerateRecoveryCodes(
        allowedRoles: Set<UserRole>,
        allowedAccountStatuses: Set<UserAccountStatus>
    ) {
        val authenticatedRequestContext = call.getAuthenticatedRequestContext()

        val authorizeResult = authenticationProvider.requireUser(
            call = call,
            allowedRoles = allowedRoles,
            allowedAccountStatuses = allowedAccountStatuses
        )

        if (authorizeResult is AppResult.Error) {
            call.respondResult(authorizeResult, appLogger, appErrorParser)
            return
        }

        val result = regenerateRecoveryCodesUseCase(
            authenticatedRequestContext = authenticatedRequestContext
        )

        if (result is AppResult.Error) {
            logErrorToAudit(
                authenticatedRequestContext = authenticatedRequestContext,
                actionType = UserAuditActionType.SELF_REGENERATE_RECOVERY_CODES,
                error = result.error
            )
        }

        call.respondResult(result, appLogger, appErrorParser) { totpRecoveryCodes ->
            totpRecoveryCodes.toTotpRecoveryCodesPayload()
        }
    }

    private fun logErrorToAudit(
        authenticatedRequestContext: AuthenticatedRequestContext,
        actionType: UserAuditActionType,
        error: AppError,
        extraMetadata: Set<AuditEventMetadata> = emptySet()
    ) {
        val errorData = auditErrorConverter.convert(error)
        val metadata = authenticatedRequestContext.clientInfo.toAuditMetadata() + errorData.metadata + extraMetadata

        auditLogger.log(
            actorId = authenticatedRequestContext.userId.asHexDashString(),
            actorType = AuditActorType.USER,
            actorUserRole = authenticatedRequestContext.userRole.serialName,
            action = actionType,
            resource = UserAuditResourceType.USER,
            resourceId = authenticatedRequestContext.userId.asHexDashString(),
            status = AuditStatus.DENIED,
            metadata = metadata
        )
    }

    companion object {
        const val SETUP_TOTP_SUMMARY = "Initiate TOTP setup"
        const val SETUP_TOTP_DESCRIPTION = "Generates a new TOTP secret and returns setup data (secret key, otpauth URL, and challenge token)."
        const val SETUP_TOTP_OPERATION_ID = "setupTotp"
        const val SETUP_TOTP_RESPONSE_OK_DESCRIPTION = "TOTP setup data generated"

        const val ENABLE_TOTP_SUMMARY = "Finalize TOTP activation"
        const val ENABLE_TOTP_DESCRIPTION = "Finalizes TOTP activation by verifying the first code from the authenticator app."
        const val ENABLE_TOTP_OPERATION_ID = "enableTotp"
        const val ENABLE_TOTP_RESPONSE_OK_DESCRIPTION = "TOTP enabled successfully"

        const val DISABLE_TOTP_SUMMARY = "Disable TOTP"
        const val DISABLE_TOTP_DESCRIPTION = "Disables TOTP and invalidates all associated recovery codes for the authenticated account."
        const val DISABLE_TOTP_OPERATION_ID = "disableTotp"
        const val DISABLE_TOTP_RESPONSE_OK_DESCRIPTION = "TOTP disabled successfully"

        const val GET_RECOVERY_CODES_SUMMARY = "Get recovery codes"
        const val GET_RECOVERY_CODES_DESCRIPTION = "Returns the current active recovery codes for the authenticated user."
        const val GET_RECOVERY_CODES_OPERATION_ID = "getRecoveryCodes"
        const val GET_RECOVERY_CODES_RESPONSE_OK_DESCRIPTION = "Current recovery codes"

        const val REGENERATE_RECOVERY_CODES_SUMMARY = "Regenerate recovery codes"
        const val REGENERATE_RECOVERY_CODES_DESCRIPTION = "Invalidates all existing recovery codes and generates a new set for the account."
        const val REGENERATE_RECOVERY_CODES_OPERATION_ID = "regenerateRecoveryCodes"
        const val REGENERATE_RECOVERY_CODES_RESPONSE_OK_DESCRIPTION = "New recovery codes generated"
    }
}