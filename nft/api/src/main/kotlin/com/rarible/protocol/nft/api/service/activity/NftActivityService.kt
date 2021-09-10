package com.rarible.protocol.nft.api.service.activity

import com.rarible.protocol.nft.core.model.ActivityResult
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.ActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.ActivitySort
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class NftActivityService(
    private val nftItemHistoryRepository: NftItemHistoryRepository
) {
    suspend fun search(
        filters: List<ActivityItemHistoryFilter>,
        sort: ActivitySort,
        size: Int
    ): List<ActivityResult> {
        val histories = filters.map { filter ->
            nftItemHistoryRepository
                .searchActivity(filter)
                .map { ActivityResult(it) }
        }
        return Flux.mergeOrdered<ActivityResult>(
            ActivityResult.comparator(sort),
            *histories.toTypedArray()
        ).take(size.toLong()).collectList().awaitFirst()
    }
}