package com.rarible.protocol.order.api.service.activity

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.AuctionActivitySort
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
        sort: AuctionActivitySort,
        size: Int
    ): List<ActivityResult> {
        val histories = historyFilters.map { filter ->
             auctionHistoryRepository
                .searchActivity(filter, size)
                .map { ActivityResult.History(it) }
        }
        return Flux.mergeOrdered<ActivityResult>(
            comparator(sort),
            *(histories).toTypedArray()
        ).take(size.toLong()).collectList().awaitFirst()
    }

    companion object {
        private val COMPARATOR = compareByDescending(ActivityResult::getDate)
            .then(compareByDescending(ActivityResult::getId))

        fun comparator(sort: AuctionActivitySort): Comparator<ActivityResult> =
            when(sort) {
                AuctionActivitySort.LATEST_FIRST -> COMPARATOR
                AuctionActivitySort.EARLIEST_FIRST -> COMPARATOR.reversed()
                else -> throw IllegalArgumentException("$sort sorting is not possible here")
            }
    }
}
