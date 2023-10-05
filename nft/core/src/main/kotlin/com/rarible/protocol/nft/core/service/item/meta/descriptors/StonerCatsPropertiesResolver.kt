package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.meta.resource.http.ExternalHttpClient
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class StonerCatsPropertiesResolver(
    private val urlService: UrlService,
    private val raribleResolver: RariblePropertiesResolver,
    private val externalHttpClient: ExternalHttpClient
) : ItemPropertiesResolver {

    override val name get() = "StonerCats"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != STONER_CAT_NFT_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId) ?: return null
        val imageUrl = properties.content.imageOriginal ?: return properties
        val etag = externalHttpClient.getEtag(url = imageUrl.url, id = itemId.decimalStringValue)

        return etag?.let {
            properties.copy(
                content = properties.content.copy(
                    imageOriginal = ContentBuilder.getItemMetaContent(
                        imageOriginal = urlService.resolvePublicHttpUrl(etag)
                    ).imageOriginal
                )
            )
        } ?: properties
    }

    companion object {
        val STONER_CAT_NFT_ADDRESS: Address = Address.apply("0xd4d871419714b778ebec2e22c7c53572b573706e")
    }
}
