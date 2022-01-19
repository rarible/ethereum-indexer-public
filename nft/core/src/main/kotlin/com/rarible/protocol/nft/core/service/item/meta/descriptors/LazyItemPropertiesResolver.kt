package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.BurnItemLazyMint
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemLazyMint
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class LazyItemPropertiesResolver(
    private val rariblePropertiesResolver: RariblePropertiesResolver,
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository
) : ItemPropertiesResolver {

    override val name: String get() = "Lazy"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val tokenUri = getUriForLazyMintedItem(itemId) ?: return null
        logMetaLoading(itemId, "found the lazy item's URI: $tokenUri")
        return rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri)
    }

    private suspend fun getUriForLazyMintedItem(itemId: ItemId): String? {
        val lazyHistories = lazyNftItemHistoryRepository.find(itemId.token, itemId.tokenId)
            .collectList()
            .awaitFirstOrNull()
            ?: return null
        // Make sure we do not return URI if the item was already burnt.
        val lazyMint = lazyHistories.filterIsInstance<ItemLazyMint>().firstOrNull()
        val lazyBurn = lazyHistories.filterIsInstance<BurnItemLazyMint>().firstOrNull()
        return when {
            lazyMint != null && lazyBurn != null -> {
                logMetaLoading(itemId, "returning nothing for a burnt item", warn = true)
                null
            }
            lazyMint != null -> lazyMint.uri
            else -> null
        }
    }
}
