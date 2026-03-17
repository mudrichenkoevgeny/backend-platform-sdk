package io.github.mudrichenkoevgeny.backend.sample

import io.github.mudrichenkoevgeny.backend.core.common.config.common.model.AppInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sample application metadata implementation.
 *
 * Exposes application name and version through the [AppInfo] contract used by SDK modules.
 */
@Singleton
class SampleAppInfo @Inject constructor() : AppInfo {
    override val version: String = "1.0.0"
    override val appName: String = "Backend-platform-sdk Sample"
}