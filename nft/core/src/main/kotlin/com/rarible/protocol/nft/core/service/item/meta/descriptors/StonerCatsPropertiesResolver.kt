package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ExternalHttpClient
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesWrapper
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class StonerCatsPropertiesResolver(
    private val ipfsService: IpfsService,
    @Qualifier("RariblePropertiesResolver") private val raribleResolver: RariblePropertiesResolver,
    private val externalHttpClient: ExternalHttpClient
) : ItemPropertiesResolver {

    override val name get() = "StonerCats"

    override suspend fun resolve(itemId: ItemId): ItemPropertiesWrapper {
        if (itemId.token != STONER_CAT_NFT_ADDRESS) {
            return wrapAsUnResolved(null)
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId).itemProperties
        val imageUrl = properties?.image ?: return wrapAsResolved(properties)
        val etag = getEtag(itemId, imageUrl)
        return wrapAsResolved(etag?.let {
            properties.copy(image = "${ipfsService.publicGateway}/ipfs/$etag")
        } ?: properties)
    }

    private suspend fun getEtag(itemId: ItemId, url: String): String? {
        val etag = try {
            externalHttpClient.get(url)
                .toBodilessEntity()
                .awaitFirstOrNull()
                ?.headers
                ?.getFirst("etag")
        } catch (e: Exception) {
            logMetaLoading(itemId, "failed to parse URI: $url: ${e.message}", warn = true)
            return null
        }
        return etag?.replace("\"", "")
    }

    companion object {

        val STONER_CAT_NFT_ADDRESS: Address = Address.apply("0xd4d871419714b778ebec2e22c7c53572b573706e")
    }
}
