extra["publishToMaven"] = true
extra["artifactId"] = "backend-platform-sdk-detekt-rules"

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.detekt.api)
}
