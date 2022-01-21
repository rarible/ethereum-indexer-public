package com.rarible.protocol.order.api.service.activity

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.repository.exchange.ActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.ActivityOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.model.OrderActivityResult
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
@CaptureSpan(type = SpanType.APP)
class OrderActivityService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderVersionRepository: OrderVersionRepository
) {
    suspend fun search(
        historyFilters: List<ActivityExchangeHistoryFilter>,
        versionFilters: List<ActivityOrderVersionFilter>,
        sort: ActivitySort,
        size: Int
    ): List<OrderActivityResult> {
        val histories = historyFilters.map { filter ->
            exchangeHistoryRepository
                .searchActivity(filter)
                .map { OrderActivityResult.History(it) }
        }
        val versions = versionFilters.map { filter ->
            orderVersionRepository
                .search(filter)
                .map { OrderActivityResult.Version(it) }
        }
        return Flux.mergeOrdered(
            ActivityResult.comparator(sort),
            *(histories + versions).toTypedArray()
        ).take(size.toLong()).collectList().awaitFirst()
    }
}
