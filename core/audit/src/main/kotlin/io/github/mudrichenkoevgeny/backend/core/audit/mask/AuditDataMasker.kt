package io.github.mudrichenkoevgeny.backend.core.audit.mask

import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskEmail
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskFullValue
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskIpAddress
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskPartialValue
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker.maskPhone
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventInternal
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEventPrivate
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.mapper.audit.toAuditEventPrivate

/**
 * Redacts sensitive fields on [AuditEventInternal] for display or API responses, converting them into [AuditEventPrivate].
 *
 * Uses the shared foundation model's sensitivity hints:
 * - [AuditEventPrivate.resourceId] is masked according to [AuditEventInternal.resourceValueSensitivity].
 * - Each metadata entry's value is masked using that entry's [AuditEventMetadata.key] sensitivity (same [AuditValueSensitivity]
 *   enum as for [AuditEventInternal.resourceValueSensitivity]).
 *
 * Masking delegates to [DataMasker] (email, phone, IP, partial/full).
 * Converts internal domain audit events ([AuditEventInternal]) into sanitized private models ([AuditEventPrivate])
 * when callers require masked views.
 */
object AuditDataMasker {

    /**
     * Transforms this [AuditEventInternal] into a sanitized [AuditEventPrivate] with sensitive fields
     * (such as `resourceId` and metadata values) redacted per their defined [AuditValueSensitivity].
     *
     * @return [AuditEventPrivate] containing masked sensitive data.
     */
    fun AuditEventInternal.maskSensitiveData(): AuditEventPrivate {
        val maskedAuditEventInternal = this.copy(
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

        return maskedAuditEventInternal.toAuditEventPrivate()
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
