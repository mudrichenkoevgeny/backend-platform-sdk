plugins {
    alias(libs.plugins.backend.platform.quality)
    alias(libs.plugins.backend.platform.kotlin)
    alias(libs.plugins.backend.platform.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.java.library)
}

dependencies {
    compileOnly(libs.detekt.api)
}
