package com.rarible.protocol.order.api.service.order

import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.repository.order.PriceOrderVersionFilter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class OrderBidsService(
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository
) {
    suspend fun findOrderBids(
        filter: PriceOrderVersionFilter,
        statuses: List<BidStatus>
    ): List<CompositeBid> {
        val totalResult = mutableListOf<CompositeBid>()
        var versions = emptyList<OrderVersion>()

        do {
            val next = if (versions.isNotEmpty()) filter.withContinuation(toContinuation(versions.last())) else filter
            versions = orderVersionRepository.search(next).collectList().awaitFirst()

            val orderHashes = versions.map { it.hash }.toHashSet()
            val orders = orderRepository.findAll(orderHashes).toList().associateBy { it.hash }

            val result = convert(versions, orders).filter { it.status in statuses }.toList()
            totalResult.addAll(result)
        } while (versions.isNotEmpty() && totalResult.size < filter.limit)

        return totalResult.take(filter.limit)
    }

    private fun convert(versions: List<OrderVersion>, orders: Map<Word, Order>): Sequence<CompositeBid> {
        val foundOrders = HashSet<Word>()

        return sequence {
            versions.forEach { version ->
                val order = orders[version.hash]
                if (order != null) {
                    val status = convert(order)

                    val calculatedStatus = if (!foundOrders.contains(version.hash) && status == BidStatus.HISTORICAL) {
                        if (order.status == OrderStatus.FILLED) BidStatus.FILLED else BidStatus.ACTIVE
                    } else {
                        status
                    }

                    foundOrders.add(version.hash)
                    yield(CompositeBid(calculatedStatus, version, order))
                }
            }
        }
    }

    private fun convert(source: Order): BidStatus {
        return when (source.status) {
            OrderStatus.CANCELLED -> BidStatus.CANCELLED
            OrderStatus.INACTIVE -> BidStatus.INACTIVE
            else -> BidStatus.HISTORICAL
        }
    }

    private fun toContinuation(orderVersion: OrderVersion): Continuation.Price {
        return Continuation.Price(orderVersion.takePriceUsd ?: BigDecimal.ZERO, orderVersion.hash)
    }
}
