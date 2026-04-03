package io.github.mudrichenkoevgeny.backend.core.audit.manager

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.common.pagination.PageParams
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.backend.core.security.masking.PayloadMaskingType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventId
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventMetadataValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.listing.AuditSortValues
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.PagedResult
import io.github.mudrichenkoevgeny.shared.foundation.core.common.domain.model.listing.SortOrder
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.resource.UserAuditResourceType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [AuditManager] implementation: delegates all operations to [AuditEventRepository] inside [dbQuery].
 */
@Singleton
class AuditManagerImpl @Inject constructor(
    private val auditRepository: AuditEventRepository
) : AuditManager {

    override suspend fun createEvent(auditEvent: AuditEvent): AppResult<AuditEvent> = dbQuery {
        auditRepository.createEvent(auditEvent)
    }

    override suspend fun getEventById(
        payloadMaskingType: PayloadMaskingType,
        eventId: AuditEventId,
    ): AppResult<AuditEvent?> = dbQuery {
        val getEventResult = auditRepository.getEventById(eventId)
        when (getEventResult) {
            is AppResult.Error -> getEventResult
            is AppResult.Success -> {
                val event = getEventResult.data
                when {
                    event == null -> getEventResult
                    payloadMaskingType == PayloadMaskingType.UNMASKED -> getEventResult
                    else -> AppResult.Success(event.maskSensitiveData())
                }
            }
        }
    }

    override suspend fun getEventsList(
        payloadMaskingType: PayloadMaskingType,
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
        val getEventsListResult = auditRepository.getEventsList(
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
        when (getEventsListResult) {
            is AppResult.Error -> getEventsListResult
            is AppResult.Success -> {
                if (payloadMaskingType == PayloadMaskingType.UNMASKED) {
                    getEventsListResult
                } else {
                    val page = getEventsListResult.data
                    AppResult.Success(
                        page.copy(
                            items = page.items.map { auditEvent ->
                                auditEvent.maskSensitiveData()
                            }
                        )
                    )
                }
            }
        }
    }

    private fun AuditEvent.maskSensitiveData(): AuditEvent {
        val resourceId = when (resource) {
            UserAuditResourceType.USER_EMAIL -> resourceId?.let { email -> DataMasker.maskEmail(email) }
            UserAuditResourceType.USER_PHONE -> resourceId?.let { phone -> DataMasker.maskPhone(phone) }
            UserAuditResourceType.USER_IDENTIFIER -> resourceId?.let { id -> DataMasker.maskId(id) }
            else -> resourceId
        }

        val maskedMetadata = metadata.map { auditEventMetadata ->
            val auditEventMetadataValue = when (auditEventMetadata.valueSensitivity) {
                AuditEventMetadataValueSensitivity.NON_SENSITIVE -> auditEventMetadata.value
                AuditEventMetadataValueSensitivity.EMAIL -> DataMasker.maskEmail(auditEventMetadata.value)
                AuditEventMetadataValueSensitivity.PHONE_NUMBER -> DataMasker.maskPhone(auditEventMetadata.value)
                AuditEventMetadataValueSensitivity.IP_ADDRESS -> DataMasker.maskIpAddress(auditEventMetadata.value)
                AuditEventMetadataValueSensitivity.PARTIAL_VALUE_MASK -> DataMasker.maskPartialValue(auditEventMetadata.value)
                AuditEventMetadataValueSensitivity.FULL_VALUE_MASK -> DataMasker.maskFullValue(auditEventMetadata.value)
            }
            auditEventMetadata.copy(value = auditEventMetadataValue)
        }.toSet()

        return copy(
            resourceId = resourceId,
            metadata = maskedMetadata
        )
    }
}