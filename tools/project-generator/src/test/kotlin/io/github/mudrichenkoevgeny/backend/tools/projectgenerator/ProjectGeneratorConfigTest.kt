package io.github.mudrichenkoevgeny.backend.tools.projectgenerator

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ProjectGeneratorConfigTest {
    private val mutatedSystemProperties = mutableListOf<String>()

    @AfterEach
    fun tearDown() {
        for (key in mutatedSystemProperties) {
            System.clearProperty(key)
        }
        mutatedSystemProperties.clear()
    }

    private fun setProp(
        key: String,
        value: String
    ) {
        System.setProperty(key, value)
        mutatedSystemProperties.add(key)
    }

    @Test
    fun `sanitizeName formats raw strings into clean hyphenated names`() {
        assertEquals("sample-app", ProjectGeneratorConfig.sanitizeName("Sample App"))
        assertEquals("my-order-service", ProjectGeneratorConfig.sanitizeName("my_order_service!!"))
        assertEquals("simple", ProjectGeneratorConfig.sanitizeName("simple"))
    }

    @Test
    fun `toPascalCase converts hyphenated names to PascalCase titles`() {
        assertEquals("SampleApp", ProjectGeneratorConfig.toPascalCase("sample-app"))
        assertEquals("MyOrderService", ProjectGeneratorConfig.toPascalCase("my-order-service"))
        assertEquals("Simple", ProjectGeneratorConfig.toPascalCase("simple"))
    }

    @Test
    fun `formatPackageName sanitizes hyphenated group IDs into valid Kotlin package names`() {
        assertEquals("com.myapp.backend", ProjectGeneratorConfig.formatPackageName("com.my-app"))
        assertEquals("org.example.service.backend", ProjectGeneratorConfig.formatPackageName("org.example.service"))
        assertEquals("com.custom.backend", ProjectGeneratorConfig.formatPackageName("com.custom.backend"))
    }

    @Test
    fun `fromSystemProperties resolves inputs correctly`() {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "sdk-root-test")
        try {
            tempDir.mkdirs()

            setProp("sdkRootDir", tempDir.absolutePath)
            setProp("projectName", "test-project")
            setProp("groupId", "com.test-company")
            setProp("linkLocal", "true")
            setProp("skipGit", "true")

            val knownKeys = listOf(
                "backendSdkVersion",
                "sharedFoundationVersion",
                "toolchainJvm",
                "kotlinVersion",
                "kspVersion",
                "detektVersion",
                "ktlintVersion",
                "coroutinesVersion",
                "serializationVersion",
                "ktorVersion",
                "daggerVersion",
                "javaxInjectVersion",
                "exposedVersion",
                "postgresqlVersion",
                "flywayVersion",
                "slf4jVersion",
                "junitVersion",
                "mockkVersion",
                "testcontainersVersion"
            )
            knownKeys.forEach { key -> setProp(key, "1.0.0") }

            val config = ProjectGeneratorConfig.fromSystemProperties()

            assertEquals("test-project", config.projectName)
            assertEquals("TestProject", config.projectTitle)
            assertEquals("com.test-company", config.groupId)
            assertEquals("com.testcompany.backend", config.packageName)
            assertTrue(config.linkLocal)
            assertTrue(config.skipGit)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `template replacer substitutes tokens correctly`() {
        val config = ProjectGeneratorConfig(
            projectName = "sample-app",
            projectTitle = "SampleApp",
            groupId = "com.example.sample",
            packageName = "com.example.sample.backend",
            targetDir = File("/tmp/target"),
            sdkRootDir = File("/tmp/sdk"),
            linkLocal = true,
            skipGit = true,
            versionProperties = mapOf(
                "backendSdkVersion" to "0.0.26",
                "sharedFoundationVersion" to "0.1.16"
            )
        )

        val replacer = TemplateReplacer(config)

        val input = "project={{PROJECT_NAME}}, title={{PROJECT_TITLE}}, package={{PACKAGE_NAME}}, " +
            "sdk={{BACKEND_SDK_VERSION}}, repo=\n{{MAVEN_LOCAL_REPOS}}"
        val result = replacer.replaceTokens(input)

        assertTrue(result.contains("project=sample-app"))
        assertTrue(result.contains("title=SampleApp"))
        assertTrue(result.contains("package=com.example.sample.backend"))
        assertTrue(result.contains("sdk=0.0.26"))
        assertTrue(result.contains("mavenLocal()"))
    }
}
