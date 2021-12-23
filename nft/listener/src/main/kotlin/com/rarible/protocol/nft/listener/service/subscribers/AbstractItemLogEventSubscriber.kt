package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumDescriptor
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.subscriber.EthereumLogEventSubscriber
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.nft.core.model.EventData
import com.rarible.protocol.nft.core.model.SubscriberGroup
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.types.ObjectId
import reactor.kotlin.core.publisher.toFlux

abstract class AbstractItemLogEventSubscriber<T : EventData>(
    group: SubscriberGroup,
    private val legacyLogEventDescriptor: LogEventDescriptor<T>
) : EthereumLogEventSubscriber() {

    private val descriptor = EthereumDescriptor(
        ethTopic = legacyLogEventDescriptor.topic,
        groupId = group,
        collection = legacyLogEventDescriptor.collection,
        contracts = emptyList(),
        entityType = ReversedEthereumLogRecord::class.java
    )

    override suspend fun getEthereumEventRecords(
        block: EthereumBlockchainBlock,
        log: EthereumBlockchainLog
    ): List<EthereumLogRecord> {
        return legacyLogEventDescriptor.convert(log.ethLog, log.ethTransaction, block.timestamp)
            .toFlux()
            .collectList()
            .map { dataCollection ->
                dataCollection.map {
                    ReversedEthereumLogRecord(
                        id = ObjectId().toHexString(),
                        log = mapLog(block, log),
                        data = it
                    )
                }
            }
            .awaitFirst()
    }

    override fun getDescriptor(): EthereumDescriptor = descriptor
}
