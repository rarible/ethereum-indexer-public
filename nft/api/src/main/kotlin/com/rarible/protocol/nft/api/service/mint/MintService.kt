package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.common.optimisticLock
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.nft.api.exceptions.LazyItemNotFoundException
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class MintService(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val historyRepository: NftItemHistoryRepository,
    private val itemReduceService: ItemReduceService
) {
    suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item = optimisticLock {
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItemHistory).awaitFirst()
        itemReduceService.onLazyItemHistories(savedItemHistory).awaitFirstOrNull()

        val itemId = ItemId(lazyItemHistory.token, lazyItemHistory.tokenId)
        itemRepository.findById(itemId).awaitFirst()
    }

    suspend fun burnLazyMint(itemId: ItemId) {
        val lazyMint = lazyNftItemHistoryRepository.findById(itemId).awaitFirstOrNull()
            ?: throw LazyItemNotFoundException(itemId)
        val log = createLogEvent(lazyMint)
        historyRepository.save(log).awaitFirstOrNull()
        itemReduceService.onItemHistories(listOf(log)).awaitFirstOrNull()
    }

    private fun createLogEvent(data: ItemLazyMint): LogEvent {
        val event = ItemTransfer(
            from = data.owner,
            owner = Address.ZERO(),
            token = data.token,
            tokenId = data.tokenId,
            date = Instant.now(),
            value = data.value
        )
        event.type = ItemType.LAZY_MINT
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
