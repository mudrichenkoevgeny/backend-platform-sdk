package io.github.mudrichenkoevgeny.backend.sample

import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.server.KtorServer
import io.github.mudrichenkoevgeny.backend.sample.appbootstrap.AppBootstrap
import io.github.mudrichenkoevgeny.backend.sample.di.AppComponent
import io.github.mudrichenkoevgeny.backend.sample.di.DaggerAppComponent
import io.github.mudrichenkoevgeny.backend.sample.lifecycle.AppShutdownHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.TimeZone

/**
 * Sample application entry point.
 *
 * This module is a reference host app that wires SDK modules together with Dagger and starts the
 * Ktor server.
 *
 * Startup flow:
 * - set UTC as a default timezone
 * - configure global holders ([PathResolverConfigHolder], [AppErrorParserConfigHolder])
 * - build the Dagger [AppComponent]
 * - run [AppBootstrap]
 * - start [KtorServer] with [module]
 * - register graceful shutdown via [AppShutdownHook]
 */
fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    PathResolverConfigHolder.set(PathResolverConfig())
    AppErrorParserConfigHolder.set(AppErrorParserConfig())

    val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val appComponent = DaggerAppComponent.factory().create(
        backgroundScope = backgroundScope
    )

    appComponent.appBootstrap().initialize()

    val server = KtorServer.create(appComponent.commonConfig()) {
        module(appComponent)
    }

    appComponent.appShutdownHook().register(server)

    server.start(wait = true)
}