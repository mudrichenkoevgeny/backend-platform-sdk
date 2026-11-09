package io.github.mudrichenkoevgeny.backend.core.settings.usecase.system.globalsettings

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.settings.global.provider.GlobalSettingsProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * System use case for seeding default global platform configurations into persistent storage.
 *
 * Runs during system bootstrap to populate default URLs, contact emails, minimum supported app versions,
 * and observability feature flags if they are missing from storage.
 */
@Singleton
public class SeedGlobalSettingsUseCase @Inject constructor(
    private val globalSettingsProvider: GlobalSettingsProvider
) {
    /**
     * Seeds missing global platform setting defaults into persistent storage.
     *
     * **Allowed Account Statuses:** Any (System initialization).
     *
     * **Security:**
     * - System-level initialization component; no caller authentication or user context required.
     *
     * **Workflow:**
     * 1. Invokes [GlobalSettingsProvider.initialize] to register missing default setting key-value pairs.
     * 2. Populates in-memory cache and broadcasts updates across nodes via Redis Pub/Sub if needed.
     *
     * @return [AppResult.Success] on successful seeding, or an [AppError] on database error.
     */
    public suspend operator fun invoke(): AppResult<Unit> = globalSettingsProvider.initialize()
}
