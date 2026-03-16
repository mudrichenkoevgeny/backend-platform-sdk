package io.github.mudrichenkoevgeny.backend.core.common.di.qualifiers

import javax.inject.Qualifier

/**
 * Qualifier for application-wide background CoroutineScope.
 *
 * Must be provided by the application (e.g. via @BindsInstance in the root component),
 * and is used by library components (audit, websocket, etc.) for background work.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class BackgroundScope