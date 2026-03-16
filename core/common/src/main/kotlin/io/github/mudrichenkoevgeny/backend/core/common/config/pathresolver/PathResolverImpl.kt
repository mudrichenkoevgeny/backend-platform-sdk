package io.github.mudrichenkoevgeny.backend.core.common.config.pathresolver

import io.github.mudrichenkoevgeny.backend.core.common.config.common.envkeys.CommonEnvKeys
import io.github.mudrichenkoevgeny.backend.core.common.error.model.CommonError
import io.github.mudrichenkoevgeny.backend.core.common.logs.AppLogger
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default [PathResolver] implementation that validates and resolves config-related filesystem paths.
 *
 * It uses [PathResolverConfig] together with the current working directory to locate the env file
 * and secrets directory, logs configuration problems via [AppLogger] as [CommonError] and fails
 * fast when required paths or environment variables are missing.
 */
@Singleton
class PathResolverImpl @Inject constructor(
    pathResolverConfig: PathResolverConfig,
    appLogger: AppLogger
) : PathResolver {

    private val envFile: File
    private val secretsDir: File

    init {
        val secretsPath = pathResolverConfig.secretsDirPath
        if (secretsPath.isNullOrBlank()) {
            val exception = IllegalStateException(
                "Required environment variable '${CommonEnvKeys.SECRETS_DIR}' is missing"
            )
            appLogger.logError(CommonError.Internal(exception))
            throw exception
        }

        secretsDir = resolveFile(pathResolverConfig.projectRoot, secretsPath)
        if (!secretsDir.exists()) {
            val exception = NoSuchFileException(secretsDir)
            appLogger.logError(CommonError.Internal(exception))
            throw exception
        }

        val envFilePath = pathResolverConfig.envFilePath
        if (envFilePath.isNullOrBlank()) {
            val exception = IllegalStateException(
                "Required environment variable '${CommonEnvKeys.ENV_FILE}' is missing"
            )
            appLogger.logError(CommonError.Internal(exception))
            throw exception
        }

        envFile = resolveFile(pathResolverConfig.projectRoot, envFilePath)
        if (!envFile.exists()) {
            val exception = NoSuchFileException(envFile)
            appLogger.logError(CommonError.Internal(exception))
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