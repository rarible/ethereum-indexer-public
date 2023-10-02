package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.protocol.contracts.external.waifus.Waifus
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
class WaifusionPropertiesResolver(
    sender: MonoTransactionSender
) : ItemPropertiesResolver {

    companion object {
        val WAIFUSION_ADDRESS = Address.apply("0x2216d47494e516d8206b70fca8585820ed3c4946")
        val IPFS_URL_PREFIX = "https://ipfs.io/ipfs/QmQuzMGqHxSXugCUvWQjDCDHWhiGB75usKbrZk6Ec6pFvw"
    }

    private val waifusion = Waifus(WAIFUSION_ADDRESS, sender)
    final val token = "waifusion"

    override val name get() = "Waifusion"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != WAIFUSION_ADDRESS) {
            return null
        }
        return waifusion.tokenNameByIndex(itemId.tokenId.value).call()
            .flatMap { tuple ->
                val name = if (tuple.isNullOrEmpty()) "Waifu #${itemId.tokenId.value}" else tuple
                waifusion.ownerOf(itemId.tokenId.value).call()
                    .map { ownerAddress ->
                        val attributes = listOf(
                            ItemAttribute("token", token),
                            ItemAttribute("owner", ownerAddress.toString())
                        )
                        ItemProperties(
                            name = name,
                            description = "Waifusion is a digital Waifu collection. There are 16,384 guaranteed-unique Waifusion NFTs. They’re just like you; a beautiful work of art, but 2-D and therefore, superior, Anon-kun.",
                            attributes = attributes,
                            rawJsonContent = null,
                            content = ContentBuilder.getItemMetaContent(
                                imageOriginal = "$IPFS_URL_PREFIX/${itemId.tokenId.value}.png"
                            )
                        )
                    }
            }.awaitFirstOrNull()
    }
}
