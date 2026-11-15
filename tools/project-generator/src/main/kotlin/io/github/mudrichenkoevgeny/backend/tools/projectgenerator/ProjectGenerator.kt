package io.github.mudrichenkoevgeny.backend.tools.projectgenerator

import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission
import kotlin.streams.asSequence

internal fun main() {
    val config = ProjectGeneratorConfig.fromSystemProperties()
    val generator = ProjectGenerator(config)
    generator.generate()
}

internal class ProjectGenerator(
    private val config: ProjectGeneratorConfig
) {
    private val replacer = TemplateReplacer(config)

    fun generate() {
        println("Scaffolding backend project '${config.projectName}' in ${config.targetDir.absolutePath}...")

        checkTargetDirectory()
        config.targetDir.mkdirs()

        copyStaticFiles()
        copyGlobalAgentRules()
        processTemplates()
        setExecutablePermissions()

        if (!config.skipGit) {
            initGitRepository()
        }

        println("Successfully scaffolded project '${config.projectName}'!")
    }

    private fun checkTargetDirectory() {
        if (!config.targetDir.exists()) return

        val existingConflicting = CONFLICTING_ENTRIES.filter { entry ->
            File(config.targetDir, entry).exists()
        }

        check(existingConflicting.isEmpty()) {
            "Target directory '${config.targetDir.absolutePath}' already contains project files " +
                "(${existingConflicting.joinToString(", ")}). Aborting."
        }
    }

    private fun copyStaticFiles() {
        for (relPath in STATIC_FILES_TO_COPY) {
            val sourceFile = File(config.sdkRootDir, relPath)
            if (sourceFile.exists()) {
                val destFile = File(config.targetDir, relPath)
                destFile.parentFile.mkdirs()
                sourceFile.copyTo(destFile, overwrite = true)
            }
        }
    }

    private fun copyGlobalAgentRules() {
        val sourceAgentGlobal = File(config.sdkRootDir, RELATIVE_AGENT_GLOBAL)
        if (sourceAgentGlobal.exists()) {
            val destAgentGlobal = File(config.targetDir, RELATIVE_AGENT_GLOBAL)
            sourceAgentGlobal.copyRecursively(destAgentGlobal, overwrite = true)
        }
    }

    private fun processTemplates() {
        val resource = javaClass.classLoader.getResource(TEMPLATES_RESOURCE_DIR)
            ?: error("Missing '$TEMPLATES_RESOURCE_DIR' resource directory in generator resources")

        val uri = resource.toURI()
        if (uri.scheme == SCHEME_JAR) {
            val fileSystem = try {
                FileSystems.getFileSystem(uri)
            } catch (_: Exception) {
                FileSystems.newFileSystem(uri, emptyMap<String, Any>())
            }
            val templatesRoot = fileSystem.getPath("/$TEMPLATES_RESOURCE_DIR")
            walkAndCopyTemplates(templatesRoot)
        } else {
            val templatesRoot = Paths.get(uri)
            walkAndCopyTemplates(templatesRoot)
        }

        createAppMigrationKeepFile()
        createSecretsKeepFile()
    }

    private fun walkAndCopyTemplates(templatesRoot: Path) {
        Files.walk(templatesRoot).use { stream ->
            stream.asSequence()
                .filter { Files.isRegularFile(it) }
                .forEach { path ->
                    val relativePath = templatesRoot.relativize(path).toString().replace('\\', '/')
                    val rawContent = String(Files.readAllBytes(path), Charsets.UTF_8)
                    val processedContent = replacer.replaceTokens(rawContent)

                    val relativeDestPath = replacer.replaceTokens(relativePath).removeSuffix(TEMPLATE_SUFFIX)
                    val destFile = File(config.targetDir, relativeDestPath)
                    destFile.parentFile.mkdirs()
                    destFile.writeText(processedContent)
                }
        }
    }

    private fun createAppMigrationKeepFile() {
        val appMigrationDir = File(config.targetDir, RELATIVE_APP_MIGRATIONS)
        appMigrationDir.mkdirs()
        val gitkeepFile = File(appMigrationDir, GITKEEP_FILE)
        if (!gitkeepFile.exists()) {
            gitkeepFile.writeText("")
        }
    }

    private fun createSecretsKeepFile() {
        val secretsDir = File(config.targetDir, RELATIVE_DEPLOY_SECRETS)
        secretsDir.mkdirs()
        val secretsGitkeep = File(secretsDir, GITKEEP_FILE)
        if (!secretsGitkeep.exists()) {
            secretsGitkeep.writeText("")
        }
    }

    private fun setExecutablePermissions() {
        val gradlew = File(config.targetDir, GRADLEW_FILE)
        if (gradlew.exists()) {
            try {
                val perms = Files.getPosixFilePermissions(gradlew.toPath()).toMutableSet()
                perms.add(PosixFilePermission.OWNER_EXECUTE)
                perms.add(PosixFilePermission.GROUP_EXECUTE)
                perms.add(PosixFilePermission.OTHERS_EXECUTE)
                Files.setPosixFilePermissions(gradlew.toPath(), perms)
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
                gradlew.setExecutable(true)
            }
        }
    }

    private fun initGitRepository() {
        try {
            val process = ProcessBuilder("git", "init")
                .directory(config.targetDir)
                .redirectErrorStream(true)
                .start()

            process.waitFor()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
            println("Warning: Failed to initialize git repository in ${config.targetDir.absolutePath}: ${e.message}")
        }
    }

    private companion object {
        private const val TEMPLATES_RESOURCE_DIR = "templates"
        private const val SCHEME_JAR = "jar"
        private const val TEMPLATE_SUFFIX = ".template"
        private const val GITKEEP_FILE = ".gitkeep"
        private const val GRADLEW_FILE = "gradlew"

        private const val RELATIVE_AGENT_GLOBAL = ".agent/global"
        private const val RELATIVE_APP_MIGRATIONS = "src/main/resources/db/migration/app"
        private const val RELATIVE_DEPLOY_SECRETS = "deploy/secrets"

        private val CONFLICTING_ENTRIES = listOf(
            "settings.gradle.kts",
            "build.gradle.kts",
            "src",
            "gradlew"
        )

        private val STATIC_FILES_TO_COPY = listOf(
            "gradlew",
            "gradlew.bat",
            "gradle/wrapper/gradle-wrapper.jar",
            "gradle/wrapper/gradle-wrapper.properties",
            "gradle/gradle-daemon-jvm.properties",
            ".editorconfig"
        )
    }
}
