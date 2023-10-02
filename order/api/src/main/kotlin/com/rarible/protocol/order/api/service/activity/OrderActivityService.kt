package com.rarible.protocol.order.api.service.activity

import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.model.OrderActivityResult
import com.rarible.protocol.order.core.model.PoolActivityResult
import com.rarible.protocol.order.core.repository.exchange.ActivityExchangeHistoryFilter
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.core.repository.order.ActivityOrderVersionFilter
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.repository.pool.PoolHistoryRepository
import kotlinx.coroutines.reactive.awaitFirst
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class OrderActivityService(
    private val exchangeHistoryRepository: ExchangeHistoryRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val poolHistoryRepository: PoolHistoryRepository
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

    suspend fun searchRight(
        historyFilter: ActivityExchangeHistoryFilter,
        sort: ActivitySort,
        size: Int
    ): List<ObjectId> {

        return exchangeHistoryRepository
            .searchShortActivity(historyFilter)
            .take(size.toLong()).collectList().awaitFirst()
    }

    suspend fun findByIds(ids: List<ObjectId>): List<OrderActivityResult> {
        val histories = exchangeHistoryRepository.findByIds(ids).map { OrderActivityResult.History(it) }
        val versions = orderVersionRepository.findByIds(ids).map { OrderActivityResult.Version(it) }
        val pools = poolHistoryRepository.findByIds(ids).map { PoolActivityResult.History(it) }
        return Flux.merge(histories, versions, pools).collectList().awaitFirst()
    }
}
