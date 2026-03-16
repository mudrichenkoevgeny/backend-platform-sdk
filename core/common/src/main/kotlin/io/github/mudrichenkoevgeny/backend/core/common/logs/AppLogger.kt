package io.github.mudrichenkoevgeny.backend.core.common.logs

import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppError

/**
 * Abstraction for application-level logging of [AppError]s.
 *
 * Implementations typically route internal failures to a system logger
 * and domain or validation problems to a business logger, so that
 * operational and product-facing signals can be handled separately.
 */
interface AppLogger {

    /**
     * Logs the given [appError] to the appropriate logger based on its type and severity.
     */
    fun logError(appError: AppError)

    companion object {
        /**
         * Name for the logger that records internal and infrastructure failures.
         */
        const val SYSTEM_LOGGER = "system"

        /**
         * Name for the logger that records business errors.
         */
        const val BUSINESS_LOGGER = "business"
    }
}