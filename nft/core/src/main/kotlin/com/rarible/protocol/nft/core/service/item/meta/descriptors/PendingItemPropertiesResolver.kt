package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.FeatureFlags
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.ReduceVersion
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.PendingItemTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class PendingItemPropertiesResolver(
    private val rariblePropertiesResolver: RariblePropertiesResolver,
    private val pendingItemTokenUriResolver: PendingItemTokenUriResolver,
    private val itemRepository: ItemRepository,
    private val ff: FeatureFlags
) : ItemPropertiesResolver {

    override val name: String get() = "Pending"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val item = itemRepository.findById(itemId).awaitSingleOrNull() ?: return null

        if (item.supply != EthUInt256.ZERO || isPendingEventsEmpty(item)) {
            // Not a pending Item, abort
            return null
        }

        val tokenUri = pendingItemTokenUriResolver.get(itemId)
        if (tokenUri == null) {
            logMetaLoading(itemId, "not found item's URI for pending item")
            return null
        } else {
            logMetaLoading(itemId, "found the pending item's URI: $tokenUri")
        }

        return rariblePropertiesResolver.resolveByTokenUri(itemId, tokenUri)
    }

    private fun isPendingEventsEmpty(item: Item): Boolean {
        return if (ff.reduceVersion == ReduceVersion.V1) {
            item.pending.isEmpty()
        } else {
            item.getPendingEvents().isEmpty()
        }
    }

}
