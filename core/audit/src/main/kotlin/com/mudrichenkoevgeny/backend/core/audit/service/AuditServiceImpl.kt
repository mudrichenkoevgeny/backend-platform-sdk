package com.mudrichenkoevgeny.backend.core.audit.service

import com.mudrichenkoevgeny.backend.core.audit.manager.AuditManager
import com.mudrichenkoevgeny.backend.core.audit.model.AuditEvent
import com.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import com.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import com.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import com.mudrichenkoevgeny.backend.core.common.result.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditServiceImpl @Inject constructor(
    private val auditManager: AuditManager,
    @param:BackgroundScope private val scope: CoroutineScope,
    private val appLogger: AppLogger
): AuditService {

    override fun log(auditEvent: AuditEvent) {
        scope.launch {
            try {
                val createEventResult = auditManager.createEvent(auditEvent)

                if (createEventResult is AppResult.Error) {
                    appLogger.logError(createEventResult.error)
                }
            } catch (t: Throwable) {
                appLogger.logError(CommonError.System(t))
            }
        }
    }

    override suspend fun awaitAll() {
        val job = scope.coroutineContext[Job]
        job?.children?.forEach { it.join() }
    }
}