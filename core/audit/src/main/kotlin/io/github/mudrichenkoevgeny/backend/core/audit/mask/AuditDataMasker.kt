package io.github.mudrichenkoevgeny.backend.core.audit.mask

import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskEmail
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskFullValue
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskIpAddress
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskPartialValue
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskPhone
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata

/**
 * Redacts sensitive fields on [AuditEvent] for display or API responses.
 *
 * Uses the shared foundation model’s sensitivity hints:
 * - [AuditEvent.resourceId] is masked according to [AuditEvent.resourceValueSensitivity].
 * - Each metadata entry’s value is masked using that entry’s [AuditEventMetadata.key] sensitivity (same [AuditValueSensitivity]
 *   enum as for [AuditEvent.resourceValueSensitivity]).
 *
 * Masking delegates to [DataMasker] (email, phone, IP, partial/full).
 * Call sites that need “masked vs unmasked” views (for example `feature/audit-api` after permission checks) apply
 * [AuditEvent.maskSensitiveData] only when the caller has masked-read permission.
 */
object AuditDataMasker {

    /**
     * Returns a copy of this event with resourceId and metadata values transformed per their sensitivity;
     * non-sensitive values are unchanged.
     */
    fun AuditEvent.maskSensitiveData(): AuditEvent {
        return this.copy(
            resourceId = resourceId?.let {
                maskBySensitivity(it, resourceValueSensitivity)
            },
            metadata = metadata.map { auditEventMetadata ->
                auditEventMetadata.copy(
                    value = maskBySensitivity(
                        value = auditEventMetadata.value,
                        sensitivity = auditEventMetadata.key.valueSensitivity
                    )
                )
            }.toSet()
        )
    }

    /**
     * Maps [AuditValueSensitivity] to the corresponding [DataMasker] helper; [AuditValueSensitivity.NON_SENSITIVE] returns [value] as-is.
     */
    fun maskBySensitivity(value: String, sensitivity: AuditValueSensitivity): String {
        return when (sensitivity) {
            AuditValueSensitivity.NON_SENSITIVE -> value
            AuditValueSensitivity.EMAIL -> maskEmail(value)
            AuditValueSensitivity.PHONE_NUMBER -> maskPhone(value)
            AuditValueSensitivity.IP_ADDRESS -> maskIpAddress(value)
            AuditValueSensitivity.PARTIAL_VALUE_MASK -> maskPartialValue(value)
            AuditValueSensitivity.FULL_VALUE_MASK -> maskFullValue(value)
        }
    }
}
