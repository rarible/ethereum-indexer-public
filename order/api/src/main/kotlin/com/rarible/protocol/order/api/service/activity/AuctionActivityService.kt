package com.rarible.protocol.order.api.service.activity

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.ActivitySort
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionHistoryFilter
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
@CaptureSpan(type = SpanType.APP)
class AuctionActivityService(
    private val auctionHistoryRepository: AuctionHistoryRepository
) {
    suspend fun search(
        historyFilters: List<ActivityAuctionHistoryFilter>,
        sort: ActivitySort,
        size: Int
    ): List<ActivityResult> {
        val histories = historyFilters.map { filter ->
             auctionHistoryRepository
                .searchActivity(filter, size)
                .map { ActivityResult.History(it) }
        }
        return Flux.mergeOrdered<ActivityResult>(
            ActivityResult.comparator(sort),
            *(histories).toTypedArray()
        ).take(size.toLong()).collectList().awaitFirst()
    }
}
