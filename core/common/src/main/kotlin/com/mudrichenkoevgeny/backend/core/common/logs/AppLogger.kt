package com.mudrichenkoevgeny.backend.core.common.logs

import com.mudrichenkoevgeny.backend.core.common.error.model.AppError

interface AppLogger {
    fun logError(appError: AppError)

    companion object {
        const val SYSTEM_LOGGER = "system"
        const val BUSINESS_LOGGER = "business"
    }
}