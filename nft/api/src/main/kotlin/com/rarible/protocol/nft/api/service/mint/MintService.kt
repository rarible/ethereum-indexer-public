package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.misc.nftOffchainEventMarks
import com.rarible.protocol.nft.core.misc.wrapWithEthereumLogRecord
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.reduce.ItemEventReduceService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipEventReduceService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component

@Component
class MintService(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val itemReduceService: ItemEventReduceService,
    private val ownershipReduceService: OwnershipEventReduceService,
    private val ownershipEventConverter: OwnershipEventConverter,
    private val itemEventConverter: ItemEventConverter
) {

    suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): Item {
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItemHistory).awaitSingle()
        val itemId = ItemId(savedItemHistory.token, savedItemHistory.tokenId)
        val logRecord = savedItemHistory.wrapWithEthereumLogRecord()
        val eventTimeMarks = nftOffchainEventMarks()
        val itemEvent = itemEventConverter.convert(logRecord, eventTimeMarks)
        val ownershipEvents = ownershipEventConverter.convert(logRecord, eventTimeMarks)
        ownershipReduceService.reduce(ownershipEvents)
        itemReduceService.reduce(listOf(requireNotNull(itemEvent)))
        return itemRepository.findById(itemId).awaitSingle()
    }

    suspend fun burnLazyMint(itemId: ItemId) {
        val lazyMint = lazyNftItemHistoryRepository.findLazyMintById(itemId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Item", itemId)
        val savedItemHistory = lazyNftItemHistoryRepository.save(
            BurnItemLazyMint(
                from = lazyMint.owner,
                token = lazyMint.token,
                tokenId = lazyMint.tokenId,
                value = lazyMint.value,
                date = nowMillis()
            )
        ).awaitFirst()
        val logRecord = savedItemHistory.wrapWithEthereumLogRecord()
        val eventTimeMarks = nftOffchainEventMarks()
        val itemEvent = itemEventConverter.convert(logRecord, eventTimeMarks)
        val ownershipEvents = ownershipEventConverter.convert(logRecord, eventTimeMarks)
        ownershipReduceService.reduce(ownershipEvents)
        itemReduceService.reduce(listOf(requireNotNull(itemEvent)))
    }
}
