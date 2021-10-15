package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.common.optimisticLock
import com.rarible.core.common.retryOptimisticLock
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class MintService(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val historyRepository: NftItemHistoryRepository,
    private val itemReduceService: ItemReduceService
) {
    suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item = optimisticLock {
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItemHistory).awaitFirst()
        itemReduceService.onLazyItemHistories(savedItemHistory).retryOptimisticLock().awaitFirstOrNull()

        val itemId = ItemId(lazyItemHistory.token, lazyItemHistory.tokenId)
        itemRepository.findById(itemId).awaitFirst()
    }

    suspend fun burnLazyMint(itemId: ItemId) {
        val lazyMint = lazyNftItemHistoryRepository.findById(itemId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Item", itemId)
        val log = createLogEvent(lazyMint)
        historyRepository.save(log).awaitFirstOrNull()
        itemReduceService.onItemHistories(listOf(log)).retryOptimisticLock().awaitFirstOrNull()
    }

    private fun createLogEvent(data: ItemLazyMint): LogEvent {
        val event = BurnItemLazyMint(
            from = data.owner,
            token = data.token,
            tokenId = data.tokenId,
            value = data.value
        )
        return LogEvent(
            data = event,
            address = Address.ZERO(),
            topic = ItemReduceService.WORD_ZERO,
            transactionHash = ItemReduceService.WORD_ZERO,
            status = LogEventStatus.CONFIRMED,
            blockNumber = -1,
            logIndex = Int.MAX_VALUE,
            index = 0,
            minorLogIndex = 0
        )
    }
}
