plugins {
    alias(libs.plugins.backend.platform.quality)
    alias(libs.plugins.backend.platform.kotlin)
    alias(libs.plugins.backend.platform.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.java.library)
}

dependencies {
    api(testFixtures(project(":core:common")))
    api(testFixtures(project(":core:database")))
    api(testFixtures(project(":core:security")))
    api(testFixtures(project(":core:audit")))
    api(testFixtures(project(":core:settings")))
    api(testFixtures(project(":core:storage")))
    api(testFixtures(project(":feature:user")))

    testImplementation(libs.konsist)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

dependencyAnalysis {
    issues {
        onUnusedDependencies {
            exclude(
                "io.github.mudrichenkoevgeny:backend-platform-sdk-core-common",
                "io.github.mudrichenkoevgeny:backend-platform-sdk-core-database",
                "io.github.mudrichenkoevgeny:backend-platform-sdk-core-security",
                "io.github.mudrichenkoevgeny:backend-platform-sdk-core-audit",
                "io.github.mudrichenkoevgeny:backend-platform-sdk-core-settings",
                "io.github.mudrichenkoevgeny:backend-platform-sdk-core-storage",
                "io.github.mudrichenkoevgeny:backend-platform-sdk-feature-user",
                "io.github.mudrichenkoevgeny:common-test-fixtures",
                ":core:common",
                ":core:database",
                ":core:security",
                ":core:audit",
                ":core:settings",
                ":core:storage",
                ":feature:user"
            )
        }
    }
}
