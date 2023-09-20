package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class PxlvrPropertiesResolver(
    urlService: UrlService,
    rawPropertiesProvider: RawPropertiesProvider,
    tokenUriResolver: BlockchainTokenUriResolver
) : AbstractRariblePropertiesResolver(urlService, rawPropertiesProvider, tokenUriResolver) {

    override val name get() = "PXLVR"

    private val originalMediaPath = listOf("media", "uri")

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != PXLVR_ADDRESS) {
            return null
        }
        return super.resolve(itemId)
    }

    override fun mapProperties(itemId: ItemId, node: ObjectNode): ItemProperties {
        val original = node.getText(originalMediaPath)
        val default = super.mapProperties(itemId, node)
        return original?.let {
            default.copy(content = ContentBuilder.getItemMetaContent(imageOriginal = original))
        } ?: default
    }

    companion object {
        val PXLVR_ADDRESS: Address = Address.apply("0xfe9ac34da3c1a0cc6990259d0c67dba704e88178")
    }
}
