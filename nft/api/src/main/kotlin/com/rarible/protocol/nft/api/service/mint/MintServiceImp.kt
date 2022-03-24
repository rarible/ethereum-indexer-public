package com.rarible.protocol.nft.api.service.mint

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.common.nowMillis
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.converters.model.OwnershipEventConverter
import com.rarible.protocol.nft.core.misc.wrapWithEthereumLogRecord
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import com.rarible.protocol.nft.core.service.item.reduce.ItemEventReduceService
import com.rarible.protocol.nft.core.service.ownership.reduce.OwnershipEventReduceService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.APP)
class MintServiceImp(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val itemRepository: ItemRepository,
    private val itemReduceService: ItemEventReduceService,
    private val ownershipReduceService: OwnershipEventReduceService,
    private val itemMetaService: ItemMetaService,
    private val ownershipEventConverter: OwnershipEventConverter,
    private val itemEventConverter: ItemEventConverter
) : MintService {

    override suspend fun createLazyNft(lazyItemHistory: ItemLazyMint): ExtendedItem {
        val savedItemHistory = lazyNftItemHistoryRepository.save(lazyItemHistory).awaitFirst()
        val itemId = ItemId(savedItemHistory.token, savedItemHistory.tokenId)
        val logRecord = savedItemHistory.wrapWithEthereumLogRecord()
        val itemEvent = itemEventConverter.convert(logRecord)
        val ownershipEvents = ownershipEventConverter.convert(logRecord)
        ownershipReduceService.reduce(ownershipEvents)
        itemReduceService.reduce(listOf(requireNotNull(itemEvent)))
        val item = itemRepository.findById(itemId).awaitFirst()
        return ExtendedItem(item, null)
    }

    override suspend fun burnLazyMint(itemId: ItemId) {
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
        itemMetaService.removeMeta(itemId)
        val itemEvent = itemEventConverter.convert(savedItemHistory.wrapWithEthereumLogRecord())
        itemReduceService.reduce(listOf(requireNotNull(itemEvent)))
    }
}
