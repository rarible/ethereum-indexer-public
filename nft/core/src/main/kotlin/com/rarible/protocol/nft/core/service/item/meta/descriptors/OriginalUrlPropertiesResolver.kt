package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.meta.resource.model.HttpUrl
import com.rarible.core.meta.resource.model.IpfsUrl
import com.rarible.core.meta.resource.model.SchemaUrl
import com.rarible.core.meta.resource.model.UrlResource
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.UrlService
import com.rarible.protocol.nft.core.service.item.meta.BlockchainTokenUriResolver
import com.rarible.protocol.nft.core.service.item.meta.ITEM_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.properties.RawPropertiesProvider
import org.springframework.stereotype.Component
import scalether.domain.Address

/**
 * Example: CryptoCube NFTs are ERC721 tokens with a custom tokenURI format.
 * It has format: "https://cryptocubes.io/api/v1/ipfs/QmbKP6tTL6getrPaoP2j5XPAj3Mgy1LTnZGRfFBYP3N1My"
 * But it resolve content not from ipfs, but via server database
 */
@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class OriginalUrlPropertiesResolver(
    urlService: UrlService,
    rawPropertiesProvider: RawPropertiesProvider,
    tokenUriResolver: BlockchainTokenUriResolver
) : AbstractRariblePropertiesResolver(urlService, rawPropertiesProvider, tokenUriResolver) {

    override val name get() = "OriginalUrl"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token !in ADDRESSES) return null
        return super.resolve(itemId)
    }

    override fun parseTokenUrl(itemId: ItemId, tokenUri: String): UrlResource {
        return super.parseTokenUrl(itemId, tokenUri).run {
            when (this) {
                is IpfsUrl -> if (originalGateway != null) HttpUrl(original) else this
                else -> this
            }
        }
    }

    companion object {
        val CRYPTO_CUBE_ADDRESS: Address = Address.apply("0xdbb163b22e839a26d2a5011841cb4430019020f9")

        val ADDRESSES: List<Address> = listOf(
            CRYPTO_CUBE_ADDRESS,
        )
    }
}
