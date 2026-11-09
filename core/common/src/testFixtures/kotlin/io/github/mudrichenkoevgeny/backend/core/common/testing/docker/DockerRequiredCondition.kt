package io.github.mudrichenkoevgeny.backend.core.common.testing.docker

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.testcontainers.DockerClientFactory

/**
 * Evaluates Docker availability for test execution.
 */
class DockerRequiredCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val isDockerAvailable = DockerClientFactory.instance().isDockerAvailable
        val isCi = System.getenv(ENV_CI) == "true"

        return when {
            isDockerAvailable -> ConditionEvaluationResult.enabled("Docker environment is available")
            isCi -> throw IllegalStateException("Docker MUST be running in CI!")
            else -> ConditionEvaluationResult.disabled("Docker environment is not available in local environment")
        }
    }

    companion object {
        private const val ENV_CI = "CI"
    }
}
