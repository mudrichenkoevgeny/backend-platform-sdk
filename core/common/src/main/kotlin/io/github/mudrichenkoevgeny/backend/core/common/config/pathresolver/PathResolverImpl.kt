package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
    class PathResolverImpl @Inject constructor(
    pathResolverConfig: PathResolverConfig,
    appLogger: AppLogger
) : PathResolver {

    private val envFile: File
    private val secretsDir: File

    init {
        val secretsPath = pathResolverConfig.secretsDirPath
        if (secretsPath == null) {
            val exception = IllegalStateException(
                "Required environment variable '${pathResolverConfig.secretsDirPath}' is missing"
            )
            appLogger.logError(CommonError.System(exception))
            throw exception
        }

        secretsDir = resolveFile(pathResolverConfig.projectRoot, secretsPath)
        if (!secretsDir.exists()) {
            val exception = NoSuchFileException(secretsDir)
            appLogger.logError(CommonError.System(exception))
            throw exception
        }

        val envFilePath = pathResolverConfig.envFilePath
        if (envFilePath == null) {
            val exception = IllegalStateException(
                "Required environment variable '${pathResolverConfig.envFilePath}' is missing"
            )
            appLogger.logError(CommonError.System(exception))
            throw exception
        }

        envFile = resolveFile(pathResolverConfig.projectRoot, envFilePath)
        if (!envFile.exists()) {
            val exception = NoSuchFileException(envFile)
            appLogger.logError(CommonError.System(exception))
            throw exception
        }
    }

    override fun getResolvedPaths(): ResolvedPaths {
        return ResolvedPaths(
            envFile = envFile,
            secretsDir = secretsDir
        )
    }

    private fun resolveFile(root: File, path: String): File {
        val file = File(path)
        return if (file.isAbsolute) {
            file
        } else {
            File(root, path)
        }
    }
}