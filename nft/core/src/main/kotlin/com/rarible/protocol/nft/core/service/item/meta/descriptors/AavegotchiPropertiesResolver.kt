package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver

class AavegotchiPropertiesResolver(
    private val openSeaPropertiesResolver: OpenSeaPropertiesResolver,
    properties: NftIndexerProperties,
) : ItemPropertiesResolver {

    private val aavegotchiImageUrlParser = AavegotchiOpenSeaImageUrlParser(properties.blockchain)

    override val name = "Aavegotchi"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        return openSeaPropertiesResolver.resolve(itemId, aavegotchiImageUrlParser)
    }
}

class AavegotchiOpenSeaImageUrlParser(
    private val blockchain: Blockchain
) : OpenSeaImageUrlParser {

    override fun parseImage(node: ObjectNode): String? {
        return when (blockchain) {
            // Original URL in Aavegotchi is broken, using image cached by OpenSea
            Blockchain.ETHEREUM -> node.getText("image_url")
            Blockchain.POLYGON -> node.getText("image")
        }
    }
}

