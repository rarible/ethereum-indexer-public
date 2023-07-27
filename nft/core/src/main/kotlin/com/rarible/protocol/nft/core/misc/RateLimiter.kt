package com.rarible.protocol.nft.core.misc

import com.rarible.core.common.nowMillis
import com.rarible.core.logging.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

class RateLimiter(
    private val maxEntities: Int,
    private val period: Long,
    private val desc: String,
) {

    private val logger by Logger()

    private val lock = Mutex()

    @Volatile
    private var nextPeriodReset = Instant.EPOCH

    @Volatile
    private var remainingEntities = 0

    suspend fun waitIfNecessary(amount: Int) {
        lock.withLock {
            if (remainingEntities < amount) {
                val timeToWait = nextPeriodReset.toEpochMilli() - nowMillis().toEpochMilli()
                if (timeToWait > 0) {
                    logger.info("Rate limit of $desc exceeded, waiting for $timeToWait ms")
                    delay(timeToWait)
                }
                remainingEntities = maxEntities
                nextPeriodReset = Instant.now().plusMillis(period)
            }
            remainingEntities -= amount
        }
    }
}
