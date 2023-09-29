package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class BerserkerDragonPropertiesResolver(
    private val raribleResolver: RariblePropertiesResolver,
) : ItemPropertiesResolver {

    override val name get() = "BerserkerDragon"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != BERSERKER_DRAGON_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId) ?: return null
        val imageUrl = properties.attributes.find { it.key == "property" }?.value ?: return properties
        val fixedAttributes = properties.attributes.filter { it.key != "property" }

        return properties.copy(
            attributes = fixedAttributes,
            content = properties.content.copy(
                imageOriginal = ContentBuilder.getItemMetaContent(
                    imageOriginal = imageUrl
                ).imageOriginal
            )
        )
    }

    companion object {
        val BERSERKER_DRAGON_ADDRESS: Address = Address.apply("0x1e602f4fe638500b4b181adfb4a0ec886afa32bd")
    }
}
