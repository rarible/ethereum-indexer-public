package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.getAttribute
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonItemPropertiesMapper
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ArtBlocksPropertiesResolver(
    urlService: UrlService,
    rawPropertiesProvider: RawPropertiesProvider,
    tokenUriResolver: BlockchainTokenUriResolver
) : AbstractRariblePropertiesResolver(urlService, rawPropertiesProvider, tokenUriResolver) {

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != ART_BLOCKS_ADDRESS) {
            return null
        }
        return super.resolve(itemId)
    }

    override fun mapProperties(itemId: ItemId, node: ObjectNode): ItemProperties {
        return JsonItemPropertiesMapper.map(itemId, node).copy(
            rights = node.getText("license"),
            attributes = getAttributes(node)
        )
    }

    private fun getAttributes(root: ObjectNode): List<ItemAttribute> {
        val node = (root.get("features") as? ObjectNode)
        node ?: return emptyList()

        val result = ArrayList<ItemAttribute>(node.size() + 2)
        node.fieldNames().forEach { result.add(node.getAttribute(it)!!) }

        root.getAttribute("project_id")?.let { result.add(it) }
        root.getAttribute("collection_name")?.let { result.add(it) }
        return result
    }

    companion object {
        val ART_BLOCKS_ADDRESS = Address.apply("0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270")
    }
}