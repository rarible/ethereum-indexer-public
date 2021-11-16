package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.contracts.external.hashmasks.Hashmasks
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

// TODO[meta]: support metadata and images of Hashmasks: https://twitter.com/thehashmasks/status/1372188306356772870
@Component
@CaptureSpan(type = META_CAPTURE_SPAN_TYPE)
class HashmasksPropertiesResolver(
    sender: MonoTransactionSender
) : ItemPropertiesResolver {

    companion object {
        val HASH_MASKS_ADDRESS = Address.apply("0xc2c747e0f7004f9e8817db2ca4997657a7746928")
    }

    private val hashmasks = Hashmasks(HASH_MASKS_ADDRESS, sender)

    override val name get() = "Hashmasks"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != HASH_MASKS_ADDRESS) {
            return null
        }
        return hashmasks.tokenNameByIndex(itemId.tokenId.value).call()
            .flatMap { tuple ->
                val name = if (tuple.isNullOrEmpty()) "Hashmask #${itemId.tokenId.value}" else tuple
                hashmasks.ownerOf(itemId.tokenId.value).call()
                    .map { ownerAddress ->
                        val attributes = listOf(
                            ItemAttribute("token", "hashmasks"),
                            ItemAttribute("owner", ownerAddress.toString())
                        )
                        ItemProperties(
                            name = name,
                            description = "Hashmasks is a living digital art collectible created by over 70 artists globally. It is a collection of 16,384 unique digital portraits. Brought to you by Suum Cuique Labs from Zug, Switzerland.",
                            attributes = attributes,
                            image = null,
                            imagePreview = null,
                            imageBig = null,
                            animationUrl = null,
                            rawJsonContent = null
                        )
                    }
            }
            .awaitFirstOrNull()
    }
}
