package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.ReduceEventListenerListener
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ItemUpdateService(
    private val itemRepository: ItemRepository,
    private val eventListenerListener: ReduceEventListenerListener,
    private val nftIndexerProperties: NftIndexerProperties,
) : EntityService<ItemId, Item> {

    override suspend fun get(id: ItemId): Item? {
        return itemRepository.findById(id).awaitFirstOrNull()
    }

    override suspend fun update(entity: Item): Item {
        val finalEntity = if (nftIndexerProperties.reduceProperties.skipOwnerships) {
            entity.copy(ownerships = emptyMap())
        } else {
            entity
        }
        val savedItem = itemRepository.save(finalEntity).awaitFirst()
        eventListenerListener.onItemChanged(savedItem).awaitFirstOrNull()
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
            append("ownerships=${item.ownerships.size}, ")
            append("last revertableEvent=${item.revertableEvents.lastOrNull()}")
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ItemUpdateService::class.java)
    }
}
