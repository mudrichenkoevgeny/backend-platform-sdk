package io.github.mudrichenkoevgeny.backend.feature.user.di.qualifiers

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
/**
 * Qualifier for the Google web client id value used by Google token verification.
 */
annotation class GoogleWebClientId