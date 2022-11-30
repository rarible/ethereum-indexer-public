package com.rarible.protocol.order.listener.service.descriptors

import com.rarible.blockchain.scanner.ethereum.model.SubscriberGroup
import com.rarible.blockchain.scanner.ethereum.subscriber.AbstractSubscriber
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.order.core.model.AuctionHistory
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.EventData
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.SubscriberGroups
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Suppress("DeprecatedCallableAddReplaceWith")
abstract class HistorySubscriber<T : EventData>(
    group: SubscriberGroup,
    collection: String,
    topic: Word,
    contracts: List<Address>
) : AbstractSubscriber<T>(group, collection, topic, contracts), LogEventDescriptor<T>
{
    @Deprecated("Should remove after switch to new scanner")
    override val topic: Word = ethereumDescriptor.ethTopic

    @Deprecated("Should remove after switch to new scanner")
    override val collection: String = ethereumDescriptor.collection

    @Deprecated("Should remove after switch to new scanner")
    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(ethereumDescriptor.contracts)

    @Deprecated("Should remove after switch to new scanner")
    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<T> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp), index, totalLogs) }.flatMapMany { it.toFlux() }
    }
}

abstract class ExchangeSubscriber<T : OrderExchangeHistory>(
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<T>(
    group = SubscriberGroups.ORDER_HISTORY,
    collection = ExchangeHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class PoolSubscriber<T : PoolHistory>(
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<T>(
    group = SubscriberGroups.POOL_HISTORY,
    collection = PoolHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class NonceSubscriber(
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<ChangeNonceHistory>(
    group = SubscriberGroups.NONCE_HISTORY,
    collection = NonceHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)

abstract class AuctionSubscriber<T : AuctionHistory>(
    topic: Word,
    contracts: List<Address>,
) : HistorySubscriber<T>(
    group = SubscriberGroups.AUCTION_HISTORY,
    collection = AuctionHistoryRepository.COLLECTION,
    topic = topic,
    contracts = contracts
)
