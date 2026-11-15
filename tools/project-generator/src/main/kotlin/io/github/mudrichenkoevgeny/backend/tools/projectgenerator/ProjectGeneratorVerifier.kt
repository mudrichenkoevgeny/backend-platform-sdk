package io.github.mudrichenkoevgeny.backend.tools.projectgenerator

import java.io.File

internal fun main() {
    val verifier = ProjectGeneratorVerifier()
    verifier.verify()
}

internal class ProjectGeneratorVerifier {
    @Suppress("NoDirectSystemEnv")
    fun verify() {
        println("Starting E2E verification for Project Generator...")

        val sdkRootDirPath = requireNotNull(System.getProperty(PROP_SDK_ROOT_DIR)) {
            "System property '$PROP_SDK_ROOT_DIR' is required"
        }
        val sdkRootDir = File(sdkRootDirPath).canonicalFile
        val buildTmpDir = File(sdkRootDir, RELATIVE_BUILD_TMP).canonicalFile

        if (buildTmpDir.exists()) {
            buildTmpDir.deleteRecursively()
        }
        buildTmpDir.mkdirs()

        val targetDir = File(buildTmpDir, TEST_PROJECT_TARGET_NAME)

        System.setProperty(PROP_PROJECT_NAME, TEST_PROJECT_NAME)
        System.setProperty(PROP_GROUP_ID, TEST_GROUP_ID)
        System.setProperty(PROP_TARGET_DIR, targetDir.absolutePath)
        System.setProperty(PROP_LINK_LOCAL, "true")
        System.setProperty(PROP_SKIP_GIT, "true")

        val config = ProjectGeneratorConfig.fromSystemProperties()
        val generator = ProjectGenerator(config)
        generator.generate()

        println("Scaffolded test project in ${targetDir.absolutePath}. Executing ./gradlew check jar...")

        val gradlewExec = if (System.getProperty(PROP_OS_NAME).lowercase().contains("win")) {
            File(targetDir, GRADLEW_BAT).absolutePath
        } else {
            File(targetDir, GRADLEW_SH).absolutePath
        }

        val processBuilder = ProcessBuilder(gradlewExec, "check", "jar", "--no-daemon", "--stacktrace")
            .directory(targetDir)
            .redirectErrorStream(true)

        if (!processBuilder.environment().containsKey(ENV_JAVA_HOME)) {
            System.getProperty(PROP_JAVA_HOME)?.let { javaHome ->
                processBuilder.environment()[ENV_JAVA_HOME] = javaHome
            }
        }

        val process = processBuilder.start()

        val reader = process.inputStream.bufferedReader()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println("  [ScaffoldBuild] $line")
        }

        val exitCode = process.waitFor()
        check(exitCode == 0) {
            "Scaffolded test project build failed with exit code $exitCode"
        }

        val appJar = File(targetDir, RELATIVE_APP_JAR)
        check(appJar.exists()) {
            "Expected build artifact '$RELATIVE_APP_JAR' does not exist in scaffolded project"
        }

        val libDir = File(targetDir, RELATIVE_LIB_DIR)
        check(libDir.exists() && (libDir.listFiles()?.isNotEmpty() == true)) {
            "Expected runtime library directory '$RELATIVE_LIB_DIR' does not exist or is empty"
        }

        println("E2E Verification SUCCESSFUL! Scaffolded project compiled, passed checks, and built app.jar.")
    }

    private companion object {
        private const val PROP_SDK_ROOT_DIR = "sdkRootDir"
        private const val PROP_PROJECT_NAME = "projectName"
        private const val PROP_GROUP_ID = "groupId"
        private const val PROP_TARGET_DIR = "targetDir"
        private const val PROP_LINK_LOCAL = "linkLocal"
        private const val PROP_SKIP_GIT = "skipGit"
        private const val PROP_OS_NAME = "os.name"
        private const val PROP_JAVA_HOME = "java.home"

        private const val ENV_JAVA_HOME = "JAVA_HOME"

        private const val GRADLEW_BAT = "gradlew.bat"
        private const val GRADLEW_SH = "gradlew"

        private const val TEST_PROJECT_NAME = "scaffold-test"
        private const val TEST_GROUP_ID = "com.example.scaffold"
        private const val TEST_PROJECT_TARGET_NAME = "scaffold-test-backend"

        private const val RELATIVE_BUILD_TMP = "build/tmp/scaffold-test"
        private const val RELATIVE_APP_JAR = "build/libs/app.jar"
        private const val RELATIVE_LIB_DIR = "build/libs/lib"
    }
}
