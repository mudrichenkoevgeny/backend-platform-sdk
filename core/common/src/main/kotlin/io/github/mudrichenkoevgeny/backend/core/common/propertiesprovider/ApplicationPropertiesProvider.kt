package io.github.mudrichenkoevgeny.backend.core.common.propertiesprovider

import java.util.Properties
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPropertiesProvider @Inject constructor() {
    private val props = Properties().apply {
        val inputStream = ApplicationPropertiesProvider::class.java
            .classLoader.getResourceAsStream(PROPERTIES_FILE_NAME)
        if (inputStream != null) {
            load(inputStream)
        }
    }

    fun get(key: String, default: String = ""): String = props.getProperty(key, default)

    val name: String get() = get(PROPERTY_NAME_APP_NAME, PROPERTY_DEFAULT_APP_NAME)
    val version: String get() = get(PROPERTY_NAME_VERSION, PROPERTY_DEFAULT_VERSION)

    companion object {
        const val PROPERTIES_FILE_NAME = "application.properties"

        const val PROPERTY_NAME_APP_NAME = "app.name"
        const val PROPERTY_DEFAULT_APP_NAME = "backend-app"

        const val PROPERTY_NAME_VERSION = "app.version"
        const val PROPERTY_DEFAULT_VERSION = "1.0.0"
    }
}