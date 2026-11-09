package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

/**
 * Default timeout and retry configuration for outbound HTTP clients.
 */
public object HttpClientConfig {
    /**
     * Maximum duration in milliseconds to wait for a full HTTP request–response round trip.
     */
    public const val REQUEST_TIMEOUT: Long = 5000L

    /**
     * Maximum duration in milliseconds to establish a TCP connection.
     */
    public const val CONNECT_TIMEOUT: Long = 2000L

    /**
     * Maximum inactivity in milliseconds on an established socket before timing out.
     */
    public const val SOCKET_TIMEOUT: Long = 5000L

    /**
     * Default number of retry attempts for failed requests.
     */
    public const val MAX_RETRIES: Int = 3
}
