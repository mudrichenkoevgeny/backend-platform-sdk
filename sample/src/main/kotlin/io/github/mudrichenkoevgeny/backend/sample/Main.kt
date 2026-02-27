package io.github.mudrichenkoevgeny.backend.sample

import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverConfig
import io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver.PathResolverConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfig
import io.github.mudrichenkoevgeny.backend.core.common.error.model.AppErrorParserConfigHolder
import io.github.mudrichenkoevgeny.backend.core.common.server.KtorServer
import io.github.mudrichenkoevgeny.backend.sample.di.DaggerAppComponent
import java.util.TimeZone

fun main() {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    PathResolverConfigHolder.set(PathResolverConfig())
    AppErrorParserConfigHolder.set(AppErrorParserConfig())

    val appComponent = DaggerAppComponent.create()

    appComponent.appBootstrap().initialize()

    val server = KtorServer.create(appComponent.commonConfig()) {
        module(appComponent)
    }

    appComponent.appShutdownHook().register(server)

    server.start(wait = true)
}