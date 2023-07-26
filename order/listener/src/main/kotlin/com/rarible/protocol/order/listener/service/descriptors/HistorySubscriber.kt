package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.SubscriberGroup
import com.rarible.blockchain.scanner.ethereum.model.SubscriberGroupAlias
import com.rarible.blockchain.scanner.ethereum.subscriber.AbstractSubscriber
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
) : AbstractSubscriber<T>(group, collection, topic, contracts, alias)

abstract class ExchangeSubscriber<T : OrderExchangeHistory>(
    name: String,
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<T>(
    group = SubscriberGroups.ORDER_HISTORY,
    alias = name,
    collection = ExchangeHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class PoolSubscriber<T : PoolHistory>(
    name: String,
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<T>(
    group = SubscriberGroups.POOL_HISTORY,
    alias = name,
    collection = PoolHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class NonceSubscriber(
    name: String,
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<ChangeNonceHistory>(
    group = SubscriberGroups.NONCE_HISTORY,
    alias = name,
    collection = NonceHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class AuctionSubscriber<T : AuctionHistory>(
    name: String,
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<T>(
    group = SubscriberGroups.AUCTION_HISTORY,
    alias = name,
    collection = AuctionHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class ApprovalSubscriber(
    name: String,
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<ApprovalHistory>(
    group = SubscriberGroups.APPROVAL_HISTORY,
    alias = name,
    collection = ApprovalHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)
