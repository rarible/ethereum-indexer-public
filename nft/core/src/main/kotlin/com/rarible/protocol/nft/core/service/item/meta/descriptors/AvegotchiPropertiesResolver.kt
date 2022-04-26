package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesWrapper
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class AvegotchiPropertiesResolver(
    private val openSeaPropertiesResolver: OpenSeaPropertiesResolver
) : ItemPropertiesResolver {

    override val name get() = "Avegotchi"

    override suspend fun resolve(itemId: ItemId): ItemPropertiesWrapper {
        if (itemId.token != AVEGOTCHI_ADDRESS) {
            return wrapAsUnResolved(null)
        }
        return openSeaPropertiesResolver.resolve(itemId)
    }

    companion object {
        val AVEGOTCHI_ADDRESS: Address = Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70")
    }
}

