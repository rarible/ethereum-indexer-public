package com.rarible.protocol.order.api.service.activity

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.ActivityResult
import com.rarible.protocol.order.core.model.AuctionActivityResult
import com.rarible.protocol.order.core.model.AuctionActivitySort
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionHistoryFilter
import com.rarible.protocol.order.core.repository.auction.ActivityAuctionOffchainFilter
import com.rarible.protocol.order.core.repository.auction.AuctionHistoryRepository
import com.rarible.protocol.order.core.repository.auction.AuctionOffchainHistoryRepository
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
@CaptureSpan(type = SpanType.APP)
class AuctionActivityService(
    private val auctionHistoryRepository: AuctionHistoryRepository,
    private val offchainHistoryRepository: AuctionOffchainHistoryRepository
) {
    suspend fun search(
        historyFilters: List<ActivityAuctionHistoryFilter>,
        offchainFilters: List<ActivityAuctionOffchainFilter>,
        sort: AuctionActivitySort,
        size: Int
    ): List<AuctionActivityResult> {
        val histories = historyFilters.map { filter ->
             auctionHistoryRepository
                .searchActivity(filter, size)
                .map { AuctionActivityResult.History(it) }
        }
        val offchains = offchainFilters.map { filter ->
            offchainHistoryRepository
                .search(filter, size)
                .map { AuctionActivityResult.OffchainHistory(it) }
        }
        return Flux.mergeOrdered(
            comparator(sort),
            *(histories + offchains).toTypedArray()
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
