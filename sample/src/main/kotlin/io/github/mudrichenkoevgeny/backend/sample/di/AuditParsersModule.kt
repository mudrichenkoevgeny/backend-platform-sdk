package io.github.mudrichenkoevgeny.backend.sample.di

import dagger.Module
import dagger.Provides
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CommonAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.action.CompositeAuditActionTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CommonAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.resource.CompositeAuditResourceTypeParser
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.audit.action.SecurityAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.security.domain.model.audit.resource.SecurityAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.audit.action.SettingsAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.core.settings.domain.model.audit.resource.SettingsAuditResourceType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.action.UserAuditActionType
import io.github.mudrichenkoevgeny.shared.foundation.feature.user.domain.model.audit.resource.UserAuditResourceType
import javax.inject.Singleton

/**
 * Host-level bindings for audit wire-type parsers.
 *
 * Supplies [CompositeAuditResourceTypeParser] and [CompositeAuditActionTypeParser] with one delegate per
 * audit enum implementation shipped with this application (cross-cutting common types plus enabled
 * features). When a new feature adds its own audit resource or action enum, register a single representative
 * entry here.
 */
@Module
class AuditParsersModule {

    @Provides
    @Singleton
    fun provideCompositeAuditResourceTypeParser() = CompositeAuditResourceTypeParser(
        setOf(
            CommonAuditResourceType.entries.first(),
            SettingsAuditResourceType.entries.first(),
            SecurityAuditResourceType.entries.first(),
            UserAuditResourceType.entries.first()
        )
    )

    @Provides
    @Singleton
    fun provideCompositeAuditActionTypeParser(): CompositeAuditActionTypeParser =
        CompositeAuditActionTypeParser(
            setOf(
                CommonAuditActionType.entries.first(),
                SettingsAuditActionType.entries.first(),
                SecurityAuditActionType.entries.first(),
                UserAuditActionType.entries.first()
            )
        )
}
