package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.mapper.EthereumLogMapper
import com.rarible.blockchain.scanner.ethereum.model.EthereumDescriptor
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.subscriber.EthereumLogEventSubscriber
import com.rarible.blockchain.scanner.framework.mapper.LogMapper
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.nft.core.model.EventData
import com.rarible.protocol.nft.core.model.SubscriberGroup
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.types.ObjectId
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigInteger

abstract class AbstractItemLogEventSubscriber<T : EventData>(
    group: SubscriberGroup,
    private val legacyLogEventDescriptor: LogEventDescriptor<T>
) : EthereumLogEventSubscriber {

    private val mapper = EthereumLogMapper()

    private val descriptor = EthereumDescriptor(
        ethTopic = legacyLogEventDescriptor.topic,
        groupId = group,
        collection = legacyLogEventDescriptor.collection,
        contracts = emptyList(),
        entityType = ReversedEthereumLogRecord::class.java
    )

    override suspend fun getEventRecords(
        block: EthereumBlockchainBlock,
        log: EthereumBlockchainLog,
        logMapper: LogMapper<EthereumBlockchainBlock, EthereumBlockchainLog, EthereumLog>,
        index: Int
    ): List<EthereumLogRecord<*>> {
        return legacyLogEventDescriptor.convert(log.ethLog, EMPTY_TRANSACTION, block.timestamp)
            .toFlux()
            .collectList()
            .map { dataCollection ->
                dataCollection.mapIndexed { minorLogIndex, data ->
                    ReversedEthereumLogRecord(
                        id = ObjectId().toHexString(),
                        log = mapper.map(block, log, index, minorLogIndex, getDescriptor()),
                        data = data
                    )
                }
            }
            .awaitFirst()
    }

    override fun getDescriptor(): EthereumDescriptor {
        return descriptor
    }

    companion object {
        val EMPTY_TRANSACTION = Transaction(
            Word.apply(ByteArray(32)),
            BigInteger.ZERO,
            Word.apply(ByteArray(32)),
            BigInteger.ZERO,
            Address.ZERO(),
            BigInteger.ZERO,
            Address.ZERO(),
            Address.ZERO(),
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            Binary.apply()
        )
    }
}
