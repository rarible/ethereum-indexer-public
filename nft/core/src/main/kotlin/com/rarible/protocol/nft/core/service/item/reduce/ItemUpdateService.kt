package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.misc.nftOffchainEventMarks
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ItemReduceEventListener
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemUpdateService(
    private val itemRepository: ItemRepository,
    private val eventListenerListener: ItemReduceEventListener
) : EntityService<ItemId, Item, ItemEvent> {

    override suspend fun get(id: ItemId): Item? {
        return itemRepository.findById(id).awaitFirstOrNull()
    }

    override suspend fun getAll(ids: Collection<ItemId>): List<Item> {
        return itemRepository.searchByIds(ids.toSet())
    }

    override suspend fun update(entity: Item, event: ItemEvent?): Item {
        val eventTimeMarks = event?.eventTimeMarks ?: nftOffchainEventMarks()
        val savedItem = itemRepository.save(entity).awaitFirst()
        eventListenerListener.onItemChanged(savedItem, eventTimeMarks).awaitFirstOrNull()
        logUpdatedItem(savedItem)
        return savedItem
    }

    private fun logUpdatedItem(item: Item) {
        logger.info(buildString {
            append("Updated item: ")
            append("id=${item.id}, ")
            append("supply=${item.supply}, ")
            append("lazySupply=${item.lazySupply}, ")
            append("lastLazyEventTimestamp=${item.lastLazyEventTimestamp}, ")
            append("deleted=${item.deleted}, ")
            append("creators=${item.creators}, ")
            append("creatorsFinal=${item.creatorsFinal}, ")
            append("isSuspiciousOnOS=${item.isSuspiciousOnOS}, ")
            append("revertableEvents=${item.revertableEvents.size}, ")
            append("last revertableEvent=${item.revertableEvents.lastOrNull()}")
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemUpdateService::class.java)
    }
}
