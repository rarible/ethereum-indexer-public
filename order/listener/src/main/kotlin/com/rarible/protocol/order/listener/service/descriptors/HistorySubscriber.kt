package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.SubscriberGroup
import com.rarible.blockchain.scanner.ethereum.model.SubscriberGroupAlias
import com.rarible.blockchain.scanner.ethereum.subscriber.AbstractSubscriber
import com.rarible.blockchain.scanner.framework.data.BlockEvent
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.blockchain.scanner.framework.data.ScanMode
import com.rarible.protocol.order.core.model.ApprovalHistory
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.SubscriberGroups
import com.rarible.protocol.order.core.repository.approval.ApprovalHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import io.daonomic.rpc.domain.Word
import scalether.domain.Address

abstract class HistorySubscriber<T : EventData>(
    group: SubscriberGroup,
    alias: SubscriberGroupAlias,
    collection: String,
    topic: Word,
    contracts: List<Address>,
    private val autoReduceService: AutoReduceService,
) : AbstractSubscriber<T>(group, collection, topic, contracts, alias) {
    override suspend fun postProcess(
        event: BlockEvent<EthereumBlockchainBlock>,
        block: FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>,
        logs: List<EthereumLogRecord>
    ): List<EthereumLogRecord> {
        val processedLogs = super.postProcess(event, block, logs)
        if (event.mode != ScanMode.REALTIME) {
            autoReduceService.autoReduce(logs)
        }
        return processedLogs
    }
}

abstract class ExchangeSubscriber<T : OrderExchangeHistory>(
    name: String,
    topic: Word,
    contracts: List<Address>,
    autoReduceService: AutoReduceService,
) : HistorySubscriber<T>(
    group = SubscriberGroups.ORDER_HISTORY,
    alias = name,
    collection = ExchangeHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts,
    autoReduceService = autoReduceService,
)

abstract class PoolSubscriber<T : PoolHistory>(
    name: String,
    topic: Word,
    contracts: List<Address>,
    autoReduceService: AutoReduceService,
) : HistorySubscriber<T>(
    group = SubscriberGroups.POOL_HISTORY,
    alias = name,
    collection = PoolHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts,
    autoReduceService = autoReduceService,
)

abstract class NonceSubscriber(
    name: String,
    topic: Word,
    contracts: List<Address>,
    autoReduceService: AutoReduceService,
) : HistorySubscriber<ChangeNonceHistory>(
    group = SubscriberGroups.NONCE_HISTORY,
    alias = name,
    collection = NonceHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts,
    autoReduceService = autoReduceService,
)

abstract class AuctionSubscriber<T : AuctionHistory>(
    name: String,
    topic: Word,
    contracts: List<Address>,
    autoReduceService: AutoReduceService,
) : HistorySubscriber<T>(
    group = SubscriberGroups.AUCTION_HISTORY,
    alias = name,
    collection = AuctionHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts,
    autoReduceService = autoReduceService,
)

abstract class ApprovalSubscriber(
    name: String,
    topic: Word,
    contracts: List<Address>,
    autoReduceService: AutoReduceService,
) : HistorySubscriber<ApprovalHistory>(
    group = SubscriberGroups.APPROVAL_HISTORY,
    alias = name,
    collection = ApprovalHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts,
    autoReduceService = autoReduceService,
)
