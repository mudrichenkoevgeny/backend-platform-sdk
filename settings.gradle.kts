pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "backend-platform-sdk"

fun registerModules(group: String, modules: List<String>) {
    modules.forEach { name ->
        val gradlePath = ":$group:$name"
        val folderPath = "$group/$name"
        include(gradlePath)
        project(gradlePath).projectDir = file(folderPath)
    }
}

val coreModules = listOf(
    "common",
    "observability",
    "database",
    "security",
    "audit",
    "storage",
    "events",
    "settings"
)
registerModules("core", coreModules)

val featureModules = listOf(
    "user"
)
registerModules("feature", featureModules)

include(":bom")

include(":sample")