package io.github.mudrichenkoevgeny.backend.core.audit.mask

import io.github.mudrichenkoevgeny.backend.core.audit.RepositoryTestAuditAction
import io.github.mudrichenkoevgeny.backend.core.audit.RepositoryTestAuditResource
import io.github.mudrichenkoevgeny.backend.core.common.mask.DataMasker
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.actor.AuditActorType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditValueSensitivity
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.AuditEventMetadata
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CommonAuditMetadataKey
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.status.AuditStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class AuditDataMaskerTest {

    @Test
    fun `maskBySensitivity leaves non sensitive unchanged`() {
        assertEquals(
            "plain",
            AuditDataMasker.maskBySensitivity("plain", AuditValueSensitivity.NON_SENSITIVE)
        )
    }

    @Test
    fun `maskBySensitivity full value mask hides non blank`() {
        assertEquals(
            DataMasker.LARGE_MASK,
            AuditDataMasker.maskBySensitivity("secret-token", AuditValueSensitivity.FULL_VALUE_MASK)
        )
    }

    @Test
    fun `maskBySensitivity full value mask returns empty for blank strings`() {
        assertEquals("", AuditDataMasker.maskBySensitivity("", AuditValueSensitivity.FULL_VALUE_MASK))
        assertEquals("", AuditDataMasker.maskBySensitivity("   ", AuditValueSensitivity.FULL_VALUE_MASK))
    }

    @Test
    fun `maskBySensitivity partial keeps first and last`() {
        assertEquals(
            "a${DataMasker.LARGE_MASK}e",
            AuditDataMasker.maskBySensitivity("abcde", AuditValueSensitivity.PARTIAL_VALUE_MASK)
        )
    }

    @Test
    fun `maskBySensitivity email uses DataMasker rules`() {
        assertEquals(
            DataMasker.maskEmail("user@example.com"),
            AuditDataMasker.maskBySensitivity("user@example.com", AuditValueSensitivity.EMAIL)
        )
    }

    @Test
    fun `maskBySensitivity phone keeps last four digits`() {
        assertEquals(
            DataMasker.maskPhone("+1 (555) 123-4567"),
            AuditDataMasker.maskBySensitivity("+1 (555) 123-4567", AuditValueSensitivity.PHONE_NUMBER)
        )
    }

    @Test
    fun `maskBySensitivity ip v4 masks tail octets`() {
        assertEquals(
            DataMasker.maskIpAddress("192.168.0.10"),
            AuditDataMasker.maskBySensitivity("192.168.0.10", AuditValueSensitivity.IP_ADDRESS)
        )
    }

    @Test
    fun `maskSensitiveData masks resourceId by resourceValueSensitivity`() {
        val event = baseEvent().copy(
            resourceId = "visible-id",
            resourceValueSensitivity = AuditValueSensitivity.FULL_VALUE_MASK
        )
        val masked = with(AuditDataMasker) { event.maskSensitiveData() }
        assertEquals(DataMasker.LARGE_MASK, masked.resourceId)
    }

    @Test
    fun `maskSensitiveData leaves null resourceId`() {
        val event = baseEvent().copy(
            resourceId = null,
            resourceValueSensitivity = AuditValueSensitivity.EMAIL
        )
        val masked = with(AuditDataMasker) { event.maskSensitiveData() }
        assertNull(masked.resourceId)
    }

    @Test
    fun `maskSensitiveData maps metadata values through key sensitivity`() {
        val code = "E_AUTH"
        val event = baseEvent().copy(
            metadata = setOf(AuditEventMetadata(CommonAuditMetadataKey.ERROR_CODE, code))
        )
        val masked = with(AuditDataMasker) { event.maskSensitiveData() }
        val meta = masked.metadata.single { it.key == CommonAuditMetadataKey.ERROR_CODE }
        assertEquals(
            AuditDataMasker.maskBySensitivity(code, CommonAuditMetadataKey.ERROR_CODE.valueSensitivity),
            meta.value
        )
    }

    @Test
    fun `maskSensitiveData preserves empty metadata`() {
        val event = baseEvent().copy(metadata = emptySet())
        val masked = with(AuditDataMasker) { event.maskSensitiveData() }
        assertEquals(emptySet<AuditEventMetadata>(), masked.metadata)
    }

    private fun baseEvent(): AuditEvent = AuditEvent(
        actorType = AuditActorType.SYSTEM,
        action = RepositoryTestAuditAction("mask_test_action"),
        resource = RepositoryTestAuditResource("mask_test_resource"),
        status = AuditStatus.SUCCESS,
        createdAt = Clock.System.now()
    )
}
