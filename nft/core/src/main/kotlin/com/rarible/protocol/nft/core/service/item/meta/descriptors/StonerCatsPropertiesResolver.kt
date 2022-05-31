package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.http.PropertiesHttpLoader
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class StonerCatsPropertiesResolver(
    private val ipfsService: IpfsService,
    private val raribleResolver: RariblePropertiesResolver,
    private val propertiesHttpLoader: PropertiesHttpLoader
) : ItemPropertiesResolver {

    override val name get() = "StonerCats"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != STONER_CAT_NFT_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId) ?: return null
        val imageUrl = properties.image ?: return properties
        val etag = propertiesHttpLoader.getEtag(url = imageUrl, id = itemId.decimalStringValue)

        return etag?.let {
            properties.copy(image = ipfsService.resolvePublicHttpUrl(etag))
        } ?: properties
    }

    companion object {
        val STONER_CAT_NFT_ADDRESS: Address = Address.apply("0xd4d871419714b778ebec2e22c7c53572b573706e")
    }
}
