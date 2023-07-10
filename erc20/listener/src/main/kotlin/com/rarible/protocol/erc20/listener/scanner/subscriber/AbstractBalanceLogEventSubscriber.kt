package com.rarible.protocol.erc20.listener.scanner.subscriber

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumDescriptor
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.subscriber.EthereumLogEventSubscriber
import com.rarible.protocol.erc20.core.metric.DescriptorMetrics
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.SubscriberGroup
import com.rarible.protocol.erc20.listener.service.IgnoredOwnersResolver
import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
import scalether.domain.Address
import scalether.domain.response.Log
import java.util.Date

abstract class AbstractBalanceLogEventSubscriber(
    ignoredOwnersResolver: IgnoredOwnersResolver,
    tokens: List<String>,
    private val metrics: DescriptorMetrics,
    group: SubscriberGroup,
    topic: Word,
    collection: String
) : EthereumLogEventSubscriber() {

    private val ignoredOwners = ignoredOwnersResolver.resolve()
    private val whitelist = tokens.map(Address::apply).toSet()

    private val descriptor = EthereumDescriptor(
        ethTopic = topic,
        groupId = group,
        collection = collection,
        contracts = emptyList(),
        entityType = ReversedEthereumLogRecord::class.java
    )

    override fun getDescriptor(): EthereumDescriptor = descriptor

    override suspend fun getEthereumEventRecords(
        block: EthereumBlockchainBlock,
        log: EthereumBlockchainLog
    ): List<EthereumLogRecord> {
        //TODO here we should send index and totalLogs
        val converted = convert(log.ethLog, Date(block.timestamp * 1000))
            .filterByOwnerAndToken()
        return converted.map {
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                log = mapLog(block, log),
                data = it
            )
        }
    }

    private fun Iterable<Erc20TokenHistory>.filterByOwnerAndToken() =
        filterTo(ArrayList()) { log ->
            val ignored = log.owner in ignoredOwners || (whitelist.isNotEmpty() && log.token !in whitelist)
            if (ignored) {
                metrics.onSkipped("ignored")
            } else {
                metrics.onSaved()
            }
            !ignored
        }

    abstract suspend fun convert(log: Log, date: Date): List<Erc20TokenHistory>
}
