package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemResolutionAbortedException
import com.rarible.protocol.nft.core.service.item.meta.getText
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class AavegotchiPropertiesResolver(
    private val openSeaPropertiesResolver: OpenSeaPropertiesResolver,
    properties: NftIndexerProperties,
) : ItemPropertiesResolver {

    private val aavegotchiImageUrlParser = AavegotchiOpenSeaImageUrlParser(properties.blockchain)

    override val name = "Aavegotchi"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != AAVEGOTCHI_ADDRESS) {
            return null
        }
        return openSeaPropertiesResolver.resolve(itemId, aavegotchiImageUrlParser)
        // if nothing found, we should abort item resolution in order to do not fall back to default OpenSea resolver
            ?: throw ItemResolutionAbortedException()
    }

    companion object {

        val AAVEGOTCHI_ADDRESS: Address = Address.apply("0x1906fd9c4ac440561f7197da0a4bd2e88df5fa70")
    }
}

class AavegotchiOpenSeaImageUrlParser(
    private val blockchain: Blockchain
) : OpenSeaImageUrlParser {

    override fun parseImage(node: ObjectNode): String? {
        return when (blockchain) {
            // Original URL in Aavegotchi is broken, using image cached by OpenSea
            Blockchain.ETHEREUM -> node.getText("image_url")
            Blockchain.POLYGON,
            Blockchain.OPTIMISM,
            Blockchain.MANTLE,
            Blockchain.HEDERA -> node.getText("image")
        }
    }
}
