package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class LegacyMintService(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val itemReduceService: ItemReduceService,
    private val itemMetaService: ItemMetaService
) : MintService {

    override suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item = optimisticLock {
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItemHistory).awaitSingle()
        val itemId = ItemId(savedItemHistory.token, savedItemHistory.tokenId)
        optimisticLock {
            itemReduceService.update(savedItemHistory.token, savedItemHistory.tokenId).awaitFirstOrNull()
        }
        itemRepository.findById(itemId).awaitSingle()
    }

    override suspend fun burnLazyMint(itemId: ItemId) {
        val lazyMint = lazyNftItemHistoryRepository.findLazyMintById(itemId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Item", itemId)
        lazyNftItemHistoryRepository.save(
            BurnItemLazyMint(
                from = lazyMint.owner,
                token = lazyMint.token,
                tokenId = lazyMint.tokenId,
                value = lazyMint.value,
                date = nowMillis()
            )
        ).awaitFirst()
        itemMetaService.removeMeta(itemId, "burn lazy mint")
        optimisticLock {
            itemReduceService.update(token = itemId.token, tokenId = itemId.tokenId).awaitFirst()
        }
    }
}
