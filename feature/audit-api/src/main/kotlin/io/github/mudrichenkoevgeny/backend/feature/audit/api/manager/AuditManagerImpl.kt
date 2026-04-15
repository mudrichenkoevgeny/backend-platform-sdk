package io.github.mudrichenkoevgeny.backend.feature.audit.api.manager

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.AuditAccessFilter
import io.github.mudrichenkoevgeny.backend.core.audit.mask.AuditDataMasker.maskSensitiveData
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionSet
import io.github.mudrichenkoevgeny.backend.core.common.permission.PermissionRequirement
import io.github.mudrichenkoevgeny.backend.feature.user.error.model.UserError
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.permission.PermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.audit.api.domain.permissions.AuditPermissionCode
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.role.UserRole
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.user.UserId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [AuditManager] implementation: delegates persistence to [AuditEventRepository] inside [dbQuery].
 */
@Singleton
class AuditManagerImpl @Inject constructor(
    private val auditRepository: AuditEventRepository
) : AuditManager {

    override suspend fun createEvent(auditEvent: AuditEvent): AppResult<AuditEvent> = dbQuery {
        auditRepository.createEvent(auditEvent)
    }

    override suspend fun getEventById(
        eventId: AuditEventId,
        userId: UserId,
        userPermissionCodes: Set<PermissionCode>
    ): AppResult<AuditEvent?> = dbQuery {
        val getEventResult = auditRepository.getEventById(eventId)

        when (getEventResult) {
            is AppResult.Error -> getEventResult
            is AppResult.Success -> {
                val auditEvent = getEventResult.data ?: return@dbQuery AppResult.Success(null)

                when (determinePermissionRequirement(auditEvent, userPermissionCodes)) {
                    PermissionRequirement.UNMASKED -> AppResult.Success(auditEvent)
                    PermissionRequirement.MASKED -> AppResult.Success(auditEvent.maskSensitiveData())
                    PermissionRequirement.FORBIDDEN -> AppResult.Error(UserError.UserMissingPermissions(userId))
                }
            }
        }
    }

    override suspend fun getEventsList(
        userPermissionCodes: Set<PermissionCode>,
        pageParams: PageParams,
        sortBy: AuditSortValues.AuditEventSortBy,
        sortOrder: SortOrder,
        actorId: String?,
        actorType: AuditActorType?,
        actorUserRole: String?,
        action: AuditActionType?,
        resource: AuditResourceType?,
        resourceId: String?,
        status: AuditStatus?,
        message: String?
    ): AppResult<PagedResult<AuditEvent>> = dbQuery {
        val accessFilter = buildAccessFilter(userPermissionCodes)

        val getEventsResult = auditRepository.getEventsList(
            accessFilter = accessFilter,
            pageParams = pageParams,
            sortBy = sortBy,
            sortOrder = sortOrder,
            actorId = actorId,
            actorType = actorType,
            actorUserRole = actorUserRole,
            action = action,
            resource = resource,
            resourceId = resourceId,
            status = status,
            message = message
        )

        when (getEventsResult) {
            is AppResult.Error -> getEventsResult
            is AppResult.Success -> {
                val pagedResult = getEventsResult.data
                val finalItems = pagedResult.items.mapNotNull { auditEvent ->
                    when (determinePermissionRequirement(auditEvent, userPermissionCodes)) {
                        PermissionRequirement.UNMASKED -> auditEvent
                        PermissionRequirement.MASKED -> auditEvent.maskSensitiveData()
                        PermissionRequirement.FORBIDDEN -> null
                    }
                }
                AppResult.Success(pagedResult.copy(items = finalItems))
            }
        }
    }

    private fun buildAccessFilter(userPermissionCodes: Set<PermissionCode>): AuditAccessFilter {
        val allowedActorTypes = mutableSetOf<AuditActorType>()
        val allowedUserRoles = mutableSetOf<String>()

        if (userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_MASKED) ||
            userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_UNMASKED)
        ) {
            allowedActorTypes.add(AuditActorType.SYSTEM)
        }

        if (userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_SERVICE_ACTOR_MASKED) ||
            userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_SERVICE_ACTOR_UNMASKED)
        ) {
            allowedActorTypes.add(AuditActorType.SERVICE)
        }

        val hasUserAccess = userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_USER_ACTOR_MASKED) ||
                userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_USER_ACTOR_UNMASKED)

        val hasStaffAccess = userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_STAFF_ACTOR_MASKED) ||
                userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_STAFF_ACTOR_UNMASKED)

        val hasAdminAccess = userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_ADMIN_ACTOR_MASKED) ||
                userPermissionCodes.contains(AuditPermissionCode.AUDIT_GET_FOR_ADMIN_ACTOR_UNMASKED)

        if (hasUserAccess || hasStaffAccess || hasAdminAccess) {
            allowedActorTypes.add(AuditActorType.USER)
            if (hasUserAccess) {
                allowedUserRoles.add(UserRole.ROLE_USER)
            }
            if (hasStaffAccess) {
                allowedUserRoles.add(UserRole.ROLE_STAFF)
            }
            if (hasAdminAccess) {
                allowedUserRoles.add(UserRole.ROLE_ADMIN)
            }
        }

        return AuditAccessFilter(allowedActorTypes, allowedUserRoles)
    }

    private fun determinePermissionRequirement(
        auditEvent: AuditEvent,
        userPermissionCodes: Set<PermissionCode>
    ): PermissionRequirement {
        val permissionSet = getRequiredPermissionsForEvent(auditEvent) ?: return PermissionRequirement.FORBIDDEN

        return when {
            userPermissionCodes.contains(permissionSet.unmasked) -> PermissionRequirement.UNMASKED
            userPermissionCodes.contains(permissionSet.masked) -> PermissionRequirement.MASKED
            else -> PermissionRequirement.FORBIDDEN
        }
    }

    private fun getRequiredPermissionsForEvent(auditEvent: AuditEvent): PermissionSet? {
        return when (auditEvent.actorType) {
            AuditActorType.SYSTEM -> PermissionSet(
                masked = AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_MASKED,
                unmasked = AuditPermissionCode.AUDIT_GET_FOR_SYSTEM_ACTOR_UNMASKED
            )
            AuditActorType.SERVICE -> PermissionSet(
                masked = AuditPermissionCode.AUDIT_GET_FOR_SERVICE_ACTOR_MASKED,
                unmasked = AuditPermissionCode.AUDIT_GET_FOR_SERVICE_ACTOR_UNMASKED
            )
            AuditActorType.USER -> {
                when (UserRole.fromValueOrNull(auditEvent.actorUserRole ?: "")) {
                    UserRole.USER -> PermissionSet(
                        masked = AuditPermissionCode.AUDIT_GET_FOR_USER_ACTOR_MASKED,
                        unmasked = AuditPermissionCode.AUDIT_GET_FOR_USER_ACTOR_UNMASKED
                    )
                    UserRole.STAFF -> PermissionSet(
                        masked = AuditPermissionCode.AUDIT_GET_FOR_STAFF_ACTOR_MASKED,
                        unmasked = AuditPermissionCode.AUDIT_GET_FOR_STAFF_ACTOR_UNMASKED
                    )
                    UserRole.ADMIN -> PermissionSet(
                        masked = AuditPermissionCode.AUDIT_GET_FOR_ADMIN_ACTOR_MASKED,
                        unmasked = AuditPermissionCode.AUDIT_GET_FOR_ADMIN_ACTOR_UNMASKED
                    )
                    null -> null
                }
            }
        }
    }
}