package io.github.mudrichenkoevgeny.backend.core.audit.service

import io.github.mudrichenkoevgeny.backend.core.audit.database.repository.AuditEventRepository
import io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers.BackgroundScope
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import io.github.mudrichenkoevgeny.backend.core.common.result.AppResult
import io.github.mudrichenkoevgeny.backend.core.database.util.dbQuery
import io.github.mudrichenkoevgeny.shared.foundation.core.audit.domain.model.event.AuditEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [AuditService] implementation: [log] launches a coroutine on the injected
 * [BackgroundScope]
 * that calls [AuditEventRepository.createEvent] inside [dbQuery]; errors are logged via [AppLogger]
 * and do not propagate.
 * [awaitAll] joins all children of the scope's job so that tests or shutdown can wait for pending writes.
 */
@Singleton
class AuditServiceImpl @Inject constructor(
    private val auditEventRepository: AuditEventRepository,
    @param:BackgroundScope private val scope: CoroutineScope,
    private val appLogger: AppLogger
) : AuditService {

    override fun log(auditEvent: AuditEvent) {
        scope.launch {
            try {
                val createEventResult = dbQuery { auditEventRepository.createEvent(auditEvent) }

                if (createEventResult is AppResult.Error) {
                    appLogger.logError(createEventResult.error)
                }
            } catch (t: Throwable) {
                appLogger.logError(CommonError.Internal(t))
            }
        }
    }

    override suspend fun awaitAll() {
        val job = scope.coroutineContext[Job]
        job?.children?.forEach { it.join() }
    }
}
