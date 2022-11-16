package com.rarible.protocol.order.listener.service.descriptors.exchange.looksrare

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.ethereum.listener.log.domain.EventData
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import scalether.domain.Address
import java.time.Instant

abstract class AbstractLooksrareExchangeDescriptor<T : EventData>(
    private val orderRepository: OrderRepository,
    private val looksrareCancelOrdersEventMetric: RegisteredCounter
) : LogEventDescriptor<T> {

    protected suspend fun cancelUserOrders(date: Instant, maker: Address, nonces: List<Long>): List<OrderCancel> {
        val result = orderRepository.findByMakeAndByCounters(Platform.LOOKSRARE, maker, nonces).map {
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
            looksrareCancelOrdersEventMetric.increment(result.size)
        }
        return result
    }
}
