package com.rarible.protocol.nft.listener.service.descriptors

import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.nft.core.model.ItemHistory
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import org.reactivestreams.Publisher
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

interface ItemHistoryLogEventDescriptor<T : ItemHistory> : LogEventDescriptor<T> {
    override val collection: String
        get() = NftItemHistoryRepository.COLLECTION

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<T> {
        return convert(log, Instant.ofEpochSecond(timestamp))
    }

    fun convert(log: Log, date: Instant): Publisher<T>
}
