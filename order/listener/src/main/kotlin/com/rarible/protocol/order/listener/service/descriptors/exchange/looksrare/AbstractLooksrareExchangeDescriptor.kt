package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant

abstract class AbstractLooksrareExchangeDescriptor<T : EventData>(
    name: String,
    topic: Word,
    contracts: List<Address>,
    private val orderRepository: OrderRepository,
    private val metrics: ForeignOrderMetrics,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderExchangeHistory>(
    name = name,
    topic = topic,
    contracts = contracts,
    autoReduceService = autoReduceService,
) {
    protected suspend fun cancelUserOrders(date: Instant, maker: Address, nonces: List<BigInteger>): List<OrderCancel> {
        return cancelOrders(date) {
            orderRepository.findByMakeAndByCounters(Platform.LOOKSRARE, maker, nonces)
        }
    }

    protected suspend fun cancelByOrderNonce(date: Instant, maker: Address, nonces: List<BigInteger>): List<OrderCancel> {
        return cancelOrders(date) {
            orderRepository.findLRByMakeAndByOrderCounters(maker, nonces)
        }
    }

    protected suspend fun cancelBySubsetNonce(date: Instant, maker: Address, nonces: List<BigInteger>): List<OrderCancel> {
        return cancelOrders(date) {
            orderRepository.findLRByMakeAndBySubsetCounters(maker, nonces)
        }
    }

    private suspend fun cancelOrders(
        date: Instant,
        find: () -> Flow<Order>
    ): List<OrderCancel> {
        val result = find().map {
            OrderCancel(
                hash = it.hash,
                maker = it.maker,
                make = it.make,
                take = it.take,
                date = date,
                source = HistorySource.LOOKSRARE
            )
        }.toList()

        if (result.isNotEmpty()) {
            // TODO or maybe nonces size?
            metrics.onOrderEventHandled(Platform.LOOKSRARE, "cancel")
        }
        return result
    }
}
