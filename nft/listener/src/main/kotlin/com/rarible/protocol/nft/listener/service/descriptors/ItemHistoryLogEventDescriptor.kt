package com.rarible.protocol.nft.listener.service.descriptors

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import org.reactivestreams.Publisher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

interface ItemHistoryLogEventDescriptor<T : ItemHistory> : LogEventDescriptor<T> {

    private val logger: Logger
        get() = LoggerFactory.getLogger(javaClass.name)

    override val collection: String
        get() = NftItemHistoryRepository.COLLECTION

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<T> {
        return convert(log, transaction, Instant.ofEpochSecond(timestamp))
    }

    fun convert(log: Log, transaction: Transaction, date: Instant): Publisher<T>

    fun mintPrice(isMint: Boolean?, value: BigInteger, countLogs: Int) : BigInteger? {
        return when (isMint) {
            true -> {
                if (countLogs > 0) {
                    value.divide(countLogs.toBigInteger())
                } else {
                    logger.warn("Count of logs must be greater than 0")
                    null
                }
            }
            else -> null
        }
    }
}
