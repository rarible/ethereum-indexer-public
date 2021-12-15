package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.PendingLogItemProperties
import com.rarible.protocol.nft.core.repository.PendingLogItemPropertiesRepository
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesService.Companion.logProperties
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = META_CAPTURE_SPAN_TYPE)
class PendingLogItemPropertiesResolver(
    private val pendingLogItemPropertiesRepository: PendingLogItemPropertiesRepository,
    private val itemRepository: ItemRepository,
    private val rariblePropertiesResolver: RariblePropertiesResolver
) : ItemPropertiesResolver {

    override val name: String get() = "Pending"

    override val canBeCached: Boolean get() = false

    suspend fun savePendingLogItemPropertiesByUri(itemId: ItemId, uri: String) {
        try {
            val itemProperties = rariblePropertiesResolver.resolveByTokenUri(itemId, uri)
            if (itemProperties == null) {
                logProperties(itemId, "no properties resolved with Rarible resolver for the pending item")
                return
            }
            val pendingLogItemProperties = PendingLogItemProperties(itemId.decimalStringValue, itemProperties)
            pendingLogItemPropertiesRepository.save(pendingLogItemProperties).awaitFirstOrNull()
        } catch (e: Exception) {
            logProperties(itemId, "failed to save pending log item properties", warn = true)
        }
    }

    // This resolver is applicable only while the item is in pending minting state.
    // Confirmed Minted items must provide properties from the contract.
    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        val item = itemRepository.findById(itemId).awaitFirstOrNull() ?: return null
        val isPendingMinting = item.pending.any { it.from == Address.ZERO() }
        if (!isPendingMinting) {
            // TODO: add a background cleanup job that removes properties for confirmed pending mints.
            return null
        }
        return pendingLogItemPropertiesRepository.findById(itemId.decimalStringValue)
            .onErrorResume { Mono.empty() }
            .awaitFirstOrNull()?.value
    }
}
