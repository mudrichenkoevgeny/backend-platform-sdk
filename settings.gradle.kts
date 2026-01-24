pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "backend-platform-sdk"

fun registerModule(name: String, path: String) {
    include(name)
    project(name).projectDir = file(path)
}

val coreModules = listOf(
    "common",
    "observability",
    "database",
    "security",
    "audit",
    "storage",
    "events",
    "crosscutting"
)
coreModules.forEach {
    registerModule(":core.$it", "core/$it")
}

val featureModules = listOf(
    "user"
)
featureModules.forEach {
    registerModule(":feature.$it", "feature/$it")
}