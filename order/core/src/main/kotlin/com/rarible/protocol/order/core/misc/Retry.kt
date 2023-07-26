package com.rarible.protocol.order.core.misc

import kotlinx.coroutines.time.delay
import java.time.Duration

object Retry {
    suspend fun <T> retry(
        attempts: Int,
        delay: Duration,
        request: suspend () -> T
    ): T {
        var lastError: Throwable? = null
        var attempt = 1
        while (attempt <= attempts) {
            try {
                return request()
            } catch (ex: SkipRetryException) {
                throw ex.throwable
            } catch (ex: Throwable) {
                attempt += 1
                lastError = ex
                delay(delay)
            }
        }
        throw lastError!!
    }

    class SkipRetryException(val throwable: Throwable) : RuntimeException()
}
