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
        if (!ART_BLOCKS_ADDRESSES.contains(itemId.token)) {
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
        node.fieldNames().forEach {
            if (node.getAttribute(it) != null) {
                result.add(node.getAttribute(it)!!)
            }
        }

        root.getAttribute("project_id")?.let { result.add(it) }
        root.getAttribute("collection_name")?.let { result.add(it) }
        return result
    }

    companion object {
        val ART_BLOCKS_ADDRESSES = listOf(
            "0x059edd72cd353df5106d2b9cc5ab83a52287ac3a",
            "0xa7d8d9ef8d8ce8992df33d8b8cf4aebabd5bd270",
            "0x99a9B7c1116f9ceEB1652de04d5969CcE509B069",
            "0x942bc2d3e7a589fe5bd4a5c6ef9727dfd82f5c8a",
            "0xea698596b6009a622c3ed00dd5a8b5d1cae4fc36"
        ).map(Address::apply).toSet()
    }
}