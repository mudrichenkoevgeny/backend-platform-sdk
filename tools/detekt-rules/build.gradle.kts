extra["publishToMaven"] = true
extra["artifactId"] = "backend-platform-sdk-detekt-rules"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shared.foundation.quality)
    alias(libs.plugins.shared.foundation.kotlin)
    alias(libs.plugins.shared.foundation.publish)
    alias(libs.plugins.java.library)
}

dependencies {
    compileOnly(libs.detekt.api)
}
