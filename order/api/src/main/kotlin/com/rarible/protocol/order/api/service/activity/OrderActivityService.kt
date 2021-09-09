package com.rarible.protocol.order.api.service.activity

import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.repository.exchange.ActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.ActivityOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.repository.sort.OrderActivitySort
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class OrderActivityService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderVersionRepository: OrderVersionRepository
) {
    suspend fun search(
        historyFilters: List<ActivityExchangeHistoryFilter>,
        versionFilters: List<ActivityOrderVersionFilter>,
        sort: OrderActivitySort,
        size: Int
    ): List<ActivityResult> {
        val histories = historyFilters.map { filter ->
            exchangeHistoryRepository
                .searchActivity(filter)
                .map { ActivityResult.History(it) }
        }
        val versions = versionFilters.map { filter ->
            orderVersionRepository
                .search(filter)
                .map { ActivityResult.Version(it) }
        }
        return Flux.mergeOrdered<ActivityResult>(
            ActivityResult.comparator(sort),
            *(histories + versions).toTypedArray()
        ).take(size.toLong()).collectList().awaitFirst()
    }
}