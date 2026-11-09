package io.github.mudrichenkoevgeny.backend.core.common.testing.docker

import org.junit.jupiter.api.extension.ExtendWith

/**
 * Indicates that a test class or method requires a running Docker daemon.
 */
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ExtendWith(DockerRequiredCondition::class)
annotation class RequiresDocker
