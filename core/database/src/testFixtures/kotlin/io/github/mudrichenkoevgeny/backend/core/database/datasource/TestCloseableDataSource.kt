package io.github.mudrichenkoevgeny.backend.core.database.datasource

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

class TestCloseableDataSource : DataSource, AutoCloseable {
    var closeCalled = false

    override fun getConnection(): Connection = throw UnsupportedOperationException()
    override fun getConnection(username: String?, password: String?): Connection = throw UnsupportedOperationException()
    override fun getLogWriter(): PrintWriter = throw UnsupportedOperationException()
    override fun setLogWriter(out: PrintWriter?) {}
    override fun getLoginTimeout(): Int = 0
    override fun setLoginTimeout(seconds: Int) {}
    override fun getParentLogger(): Logger = throw UnsupportedOperationException()
    override fun <T : Any> unwrap(iface: Class<T>): T = throw UnsupportedOperationException()
    override fun isWrapperFor(iface: Class<*>): Boolean = false
    override fun close() {
        closeCalled = true
    }
}
