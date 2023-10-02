package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumDescriptor
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.subscriber.EthereumLogEventSubscriber
import com.rarible.blockchain.scanner.framework.data.BlockEvent
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.blockchain.scanner.framework.data.ScanMode
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.nft.core.model.EventData
import com.rarible.protocol.nft.core.model.SubscriberGroup
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import reactor.kotlin.core.publisher.toFlux

abstract class AbstractItemLogEventSubscriber<T : EventData>(
    group: SubscriberGroup,
    private val legacyLogEventDescriptor: LogEventDescriptor<T>,
    private val autoReduceService: AutoReduceService,
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
        // TODO here we should send index and totalLogs
        return legacyLogEventDescriptor.convert(log.ethLog, log.ethTransaction, block.timestamp, 0, 0)
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

    override suspend fun postProcess(
        event: BlockEvent<EthereumBlockchainBlock>,
        block: FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>,
        logs: List<EthereumLogRecord>
    ): List<EthereumLogRecord> {
        if (event.mode != ScanMode.REALTIME) {
            autoReduceService.autoReduce(logs)
        }
        return logs
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AbstractItemLogEventSubscriber::class.java)
    }
}
