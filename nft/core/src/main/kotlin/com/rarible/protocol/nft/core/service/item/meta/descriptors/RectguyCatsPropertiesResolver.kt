package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.parser.UrlParser
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class RectguyCatsPropertiesResolver(
    private val urlParser: UrlParser,
    private val raribleResolver: RariblePropertiesResolver
) : ItemPropertiesResolver {

    override val name get() = "Rectguy"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != RECTGUY_NFT_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "Resolving $name Nft properties")
        val properties = raribleResolver.resolve(itemId) ?: return null

        val content = properties.content

        return properties.copy(
            content = content.copy(
                imagePreview = replaceIpfsGateway(content.imagePreview),
                imageBig = replaceIpfsGateway(content.imageBig),
                imageOriginal = replaceIpfsGateway(content.imageOriginal),
                videoOriginal = replaceIpfsGateway(content.videoOriginal),
            )
        )
    }

    private fun replaceIpfsGateway(content: EthMetaContent?): EthMetaContent? {
        content ?: return null

        val res = urlParser.parse(content.url)
        if (res is IpfsUrl && RECTGUY_GATEWAY == res.originalGateway) {
            return content.copy(url = res.toSchemaUrl())
        }

        return content
    }

    companion object {

        val RECTGUY_NFT_ADDRESS: Address = Address.apply("0xb852c6b5892256c264cc2c888ea462189154d8d7")
        val RECTGUY_GATEWAY = "https://rektguy.mypinata.cloud"
    }
}
