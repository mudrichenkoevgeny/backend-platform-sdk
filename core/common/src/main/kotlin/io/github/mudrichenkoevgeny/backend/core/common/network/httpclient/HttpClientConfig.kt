package io.github.mudrichenkoevgeny.backend.core.common.network.httpclient

/**
 * Default timeout and retry configuration for outbound HTTP clients.
 */
object HttpClientConfig {

    /**
     * Maximum duration in milliseconds to wait for a full HTTP request–response round trip.
     */
    const val REQUEST_TIMEOUT = 5000L

    /**
     * Maximum duration in milliseconds to establish a TCP connection.
     */
    const val CONNECT_TIMEOUT = 2000L

    /**
     * Maximum inactivity in milliseconds on an established socket before timing out.
     */
    const val SOCKET_TIMEOUT = 5000L

    /**
     * Default number of retry attempts for failed requests.
     */
    const val MAX_RETRIES = 3
}