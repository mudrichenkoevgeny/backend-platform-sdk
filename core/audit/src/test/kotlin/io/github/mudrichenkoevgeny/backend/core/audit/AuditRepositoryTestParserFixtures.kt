package io.github.mudrichenkoevgeny.backend.core.audit

import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireAction
import io.github.mudrichenkoevgeny.backend.core.audit.domain.wire.AuditWireResource
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser

/**
 * Test delegates that map any stored wire string to [AuditWireAction] / [AuditWireResource], matching
 * repository row hydration with a host-configured composite parser.
 */
internal object AcceptAnyWireAuditActionDelegate : AuditActionType {
    override val serialName: String = "__test_action_delegate__"
    override fun parseOrNull(value: String): AuditActionType? = AuditWireAction(value)
    override fun parseOrThrow(value: String): AuditActionType = AuditWireAction(value)
}

internal object AcceptAnyWireAuditResourceDelegate : AuditResourceType {
    override val serialName: String = "__test_resource_delegate__"
    override fun parseOrNull(value: String): AuditResourceType? = AuditWireResource(value)
    override fun parseOrThrow(value: String): AuditResourceType = AuditWireResource(value)
}

internal fun compositeAuditActionTypeParserForRepositoryTests(): CompositeAuditActionTypeParser =
    CompositeAuditActionTypeParser(setOf(AcceptAnyWireAuditActionDelegate))

internal fun compositeAuditResourceTypeParserForRepositoryTests(): CompositeAuditResourceTypeParser =
    CompositeAuditResourceTypeParser(setOf(AcceptAnyWireAuditResourceDelegate))
