package io.github.mudrichenkoevgeny.backend.core.common.config.env

/**
 * Abstraction over environment configuration sources.
 *
 * Implementations may combine process environment variables, `.env` files and external
 * secret stores, but the contract stays consistent for consumers.
 */
interface EnvReader {

    /**
     * Returns the value for the given environment [key] or throws if it does not exist.
     *
     * @param key environment variable name.
     * @throws IllegalStateException when the key is missing.
     */
    fun getByKey(key: String): String

    /**
     * Returns the value for the given environment [key] or `null` when it is not set.
     *
     * @param key environment variable name.
     */
    fun getByKeyOrNull(key: String): String?

    /**
     * Reads a secret value from a file under the configured secrets directory.
     *
     * Implementations may also support absolute paths.
     *
     * @param relativeFile relative or absolute path to the secret file.
     * @return secret contents with surrounding whitespace trimmed.
     */
    fun readSecret(relativeFile: String): String
}