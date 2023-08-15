package com.rarible.protocol.order.core.model

import com.rarible.looksrare.client.model.v2.LooksrareObject
import com.rarible.protocol.order.core.misc.looksrareInfo
import org.slf4j.LoggerFactory
import java.time.Instant

data class LooksrareV2Cursor(
    val createdAfter: Instant,
    val nextId: String? = null,
    val maxSeenCreated: Instant? = null
) {
    fun next(results: List<LooksrareObject>): LooksrareV2Cursor {
        val max = results.maxByOrNull { it.createdAt } ?: return this
        val min = results.minByOrNull { it.createdAt } ?: return this

        // Need to save max seen created order to continue from it after we fetch all old orders
        val savingMaxSeenCreated = maxSeenCreated
            ?.let { maxOf(max.createdAt, it) }
        // "createdAfter" is current checkpoint, sometimes LR api returns orders from the past somehow,
        // we don't need to save checkpoint from the past
            ?: maxOf(max.createdAt, createdAfter)

        return if (min.createdAt > createdAfter) {
            logger.looksrareInfo("Still go deep, min createdAfter=${min.createdAt}")
            LooksrareV2Cursor(
                createdAfter = createdAfter,
                nextId = min.id,
                maxSeenCreated = savingMaxSeenCreated
            )
        } else {
            logger.looksrareInfo("Load all, max createdAfter=$savingMaxSeenCreated")
            LooksrareV2Cursor(createdAfter = savingMaxSeenCreated)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LooksrareV2Cursor::class.java)

        fun default() = LooksrareV2Cursor(Instant.now())
    }
}
