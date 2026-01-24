plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.java.library)
}

dependencies {
    implementation(project(":core.common"))
    implementation(project(":core.security"))
    implementation(project(":core.audit"))

    implementation(libs.kotlinx.serialization.json)
    
    kapt(libs.dagger.compiler)
    implementation(libs.dagger)
}

tasks.test {
    useJUnitPlatform()
}