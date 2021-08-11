package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.common.optimisticLock
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class MintService(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val itemReduceService: ItemReduceService
) {
    suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item = optimisticLock {
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItemHistory).awaitFirst()
        itemReduceService.onLazyItemHistories(savedItemHistory).awaitFirstOrNull()

        val itemId = ItemId(lazyItemHistory.token, lazyItemHistory.tokenId)
        itemRepository.findById(itemId).awaitFirst()
    }
}