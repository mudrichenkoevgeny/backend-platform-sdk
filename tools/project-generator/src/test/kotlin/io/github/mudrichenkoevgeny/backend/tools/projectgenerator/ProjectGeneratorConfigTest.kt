package io.github.mudrichenkoevgeny.backend.tools.projectgenerator

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

internal class ProjectGeneratorConfigTest {
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

            val testVersions = createTestVersionMap()
            testVersions.forEach { (key, value) -> setProp(key, value) }

            val config = ProjectGeneratorConfig.fromSystemProperties()

            assertEquals("test-project", config.projectName)
            assertEquals("TestProject", config.projectTitle)
            assertEquals("com.test-company", config.groupId)
            assertEquals("com.testcompany.backend", config.packageName)
            assertTrue(config.linkLocal)
            assertTrue(config.skipGit)
            assertEquals(testVersions, config.versionProperties)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `template replacer substitutes tokens correctly`() {
        val config = createTestConfig()
        val replacer = TemplateReplacer(config)

        val rawTemplate = "project={{PROJECT_NAME}}, title={{PROJECT_TITLE}}, pkg={{PACKAGE_NAME}}, " +
            "sdk={{BACKEND_SDK_VERSION}}, repo=\n{{MAVEN_LOCAL_REPOS}}"
        val evaluated = replacer.replaceTokens(rawTemplate)

        val expected = "project=sample-app, title=SampleApp, pkg=com.example.sample.backend, " +
            "sdk=1.0.0, repo=\n        mavenLocal()\n"
        assertEquals(expected, evaluated)
    }

    private fun createTestConfig(): ProjectGeneratorConfig =
        ProjectGeneratorConfig(
            projectName = "sample-app",
            projectTitle = "SampleApp",
            groupId = "com.example.sample",
            packageName = "com.example.sample.backend",
            targetDir = File("build/tmp/test"),
            sdkRootDir = File("."),
            linkLocal = true,
            skipGit = true,
            versionProperties = createTestVersionMap()
        )

    private fun createTestVersionMap(): Map<String, String> =
        mapOf(
            "backendSdkVersion" to "1.0.0",
            "sharedFoundationVersion" to "1.0.0",
            "toolchainJvm" to "25",
            "kotlinVersion" to "1.0.0",
            "kspVersion" to "1.0.0",
            "detektVersion" to "1.0.0",
            "ktlintVersion" to "1.0.0",
            "coroutinesVersion" to "1.0.0",
            "serializationVersion" to "1.0.0",
            "ktorVersion" to "1.0.0",
            "daggerVersion" to "1.0.0",
            "javaxInjectVersion" to "1.0.0",
            "exposedVersion" to "1.0.0",
            "postgresqlVersion" to "1.0.0",
            "flywayVersion" to "1.0.0",
            "slf4jVersion" to "1.0.0",
            "junitVersion" to "1.0.0",
            "mockkVersion" to "1.0.0",
            "testcontainersVersion" to "1.0.0"
        )
}
