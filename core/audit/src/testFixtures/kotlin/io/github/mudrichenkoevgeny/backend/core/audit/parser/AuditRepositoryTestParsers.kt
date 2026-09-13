package io.github.mudrichenkoevgeny.backend.core.audit.parser

import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.action.AcceptAnyStringAuditActionDelegate
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.metadata.AcceptAnyStringAuditMetadataKeyDelegate
import io.github.mudrichenkoevgeny.backend.core.audit.domain.model.resource.AcceptAnyStringAuditResourceDelegate
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.metadata.CompositeAuditMetadataKeyParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser

fun compositeAuditActionTypeParserForRepositoryTests(): CompositeAuditActionTypeParser =
    CompositeAuditActionTypeParser(setOf(AcceptAnyStringAuditActionDelegate))

fun compositeAuditResourceTypeParserForRepositoryTests(): CompositeAuditResourceTypeParser =
    CompositeAuditResourceTypeParser(setOf(AcceptAnyStringAuditResourceDelegate))

fun compositeAuditMetadataKeyParserForRepositoryTests(): CompositeAuditMetadataKeyParser =
    CompositeAuditMetadataKeyParser(setOf(AcceptAnyStringAuditMetadataKeyDelegate))
