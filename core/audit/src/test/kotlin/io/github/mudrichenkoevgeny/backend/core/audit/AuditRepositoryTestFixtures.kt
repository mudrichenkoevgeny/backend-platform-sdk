package io.github.mudrichenkoevgeny.backend.core.audit

import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.AuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.AuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser

/**
 * Minimal [AuditActionType] / [AuditResourceType] for repository tests only (arbitrary persisted string).
 */
internal data class RepositoryTestAuditAction(
    override val serialName: String
) : AuditActionType {
    override fun parseOrNull(value: String): AuditActionType? = RepositoryTestAuditAction(value)
    override fun parseOrThrow(value: String): AuditActionType = RepositoryTestAuditAction(value)
}

internal data class RepositoryTestAuditResource(
    override val serialName: String
) : AuditResourceType {
    override fun parseOrNull(value: String): AuditResourceType? = RepositoryTestAuditResource(value)
    override fun parseOrThrow(value: String): AuditResourceType = RepositoryTestAuditResource(value)
}

/**
 * Test parser delegates: any DB string round-trips as the test types above.
 */
internal object AcceptAnyStringAuditActionDelegate : AuditActionType {
    override val serialName: String = "__test_action_delegate__"
    override fun parseOrNull(value: String): AuditActionType? = RepositoryTestAuditAction(value)
    override fun parseOrThrow(value: String): AuditActionType = RepositoryTestAuditAction(value)
}

internal object AcceptAnyStringAuditResourceDelegate : AuditResourceType {
    override val serialName: String = "__test_resource_delegate__"
    override fun parseOrNull(value: String): AuditResourceType? = RepositoryTestAuditResource(value)
    override fun parseOrThrow(value: String): AuditResourceType = RepositoryTestAuditResource(value)
}

internal fun compositeAuditActionTypeParserForRepositoryTests(): CompositeAuditActionTypeParser =
    CompositeAuditActionTypeParser(setOf(AcceptAnyStringAuditActionDelegate))

internal fun compositeAuditResourceTypeParserForRepositoryTests(): CompositeAuditResourceTypeParser =
    CompositeAuditResourceTypeParser(setOf(AcceptAnyStringAuditResourceDelegate))
