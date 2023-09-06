package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumDescriptor
import com.rarible.blockchain.scanner.framework.subscriber.LogEventSubscriber
import com.rarible.protocol.nft.core.model.SetBaseUriRecord
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc721.TokenUriRevealLogDescriptor
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import reactor.kotlin.core.publisher.toFlux

@Component
class TokenUriRevealLogSubscriber(private val logDescriptor: TokenUriRevealLogDescriptor) :
    LogEventSubscriber<EthereumBlockchainBlock, EthereumBlockchainLog, SetBaseUriRecord, EthereumDescriptor> {
    private val descriptor = EthereumDescriptor(
        ethTopic = logDescriptor.topic,
        groupId = SubscriberGroups.SET_BASE_URI,
        collection = logDescriptor.collection,
        contracts = emptyList(),
        entityType = SetBaseUriRecord::class.java
    )

    override suspend fun getEventRecords(
        block: EthereumBlockchainBlock,
        log: EthereumBlockchainLog
    ): List<SetBaseUriRecord> {
        return logDescriptor.convert(log.ethLog, log.ethTransaction, block.timestamp, 0, 0)
            .toFlux()
            .map {
                SetBaseUriRecord(
                    hash = log.ethTransaction.hash().toString(),
                    address = it.id
                )
            }
            .collectList()
            .awaitFirst()
    }

    override fun getDescriptor(): EthereumDescriptor = descriptor
}
