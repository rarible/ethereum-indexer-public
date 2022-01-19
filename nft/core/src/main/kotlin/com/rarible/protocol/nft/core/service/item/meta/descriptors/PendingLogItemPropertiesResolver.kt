package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class PendingLogItemPropertiesResolver(
    private val itemRepository: ItemRepository,
    private val rariblePropertiesResolver: RariblePropertiesResolver
) : ItemPropertiesResolver {

    override val name: String get() = "Pending"

    // This resolver is applicable only while the item is in pending minting state.
    // Confirmed Minted items must provide properties from the contract.
    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val item = itemRepository.findById(itemId).awaitFirstOrNull() ?: return null
        val tokenUri = item.pending.mapNotNull { it.tokenUri }.firstOrNull()
            ?: item.getPendingEvents().asSequence()
                .filterIsInstance<ItemEvent.ItemMintEvent>()
                .mapNotNull { it.tokenUri }
                .firstOrNull()
        if (tokenUri == null) {
            if (item.getPendingEvents().any { it is ItemEvent.ItemMintEvent }) {
                logMetaLoading(itemId, "no tokenURI found for pending item", warn = true)
            }
            return null
        }
        logMetaLoading(itemId, "pending item has tokenURI '$tokenUri'")
        return rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri)
    }
}
