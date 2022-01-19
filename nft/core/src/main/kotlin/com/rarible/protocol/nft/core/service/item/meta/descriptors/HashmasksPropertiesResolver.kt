package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.contracts.external.hashmasks.Hashmasks
import com.rarible.protocol.contracts.external.hashmasks.MasksRegistry
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class HashmasksPropertiesResolver(
    sender: MonoTransactionSender,
    private val ipfsService: IpfsService
) : ItemPropertiesResolver {

    companion object {
        val HASH_MASKS_ADDRESS = Address.apply("0xc2c747e0f7004f9e8817db2ca4997657a7746928")
        val HASH_MASKS_REGISTRY_ADDRESS = Address.apply("0x185c8078285a3de3ec9a2c203ad12853f03c462d")
    }

    private val hashmasks = Hashmasks(HASH_MASKS_ADDRESS, sender)
    private val hashmasksRegistry = MasksRegistry(HASH_MASKS_REGISTRY_ADDRESS, sender)

    override val name get() = "Hashmasks"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != HASH_MASKS_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "resolving Hashmasks properties")
        val tokenName = hashmasks.tokenNameByIndex(itemId.tokenId.value)
            .call().awaitFirstOrNull() ?: return null
        val ipfsHash = hashmasksRegistry.getIPFSHashOfMaskId(itemId.tokenId.value)
            .call().awaitFirstOrNull() ?: return null
        val traitsOfMaskId = hashmasksRegistry.getTraitsOfMaskId(itemId.tokenId.value)
            .call().awaitFirstOrNull() ?: return null
        val character = traitsOfMaskId._1()
        val mask = traitsOfMaskId._2()
        val eyeColor = traitsOfMaskId._3()
        val skinColor = traitsOfMaskId._4()
        val item = traitsOfMaskId._5()
        val attributes = mapOf(
            "character" to character,
            "mask" to mask,
            "eyeColor" to eyeColor,
            "skinColor" to skinColor,
            "item" to item
        ).map { ItemAttribute(it.key, it.value) }
        val imageUrl = ipfsService.resolveHttpUrl(ipfsHash)
        return ItemProperties(
            name = tokenName,
            description = "Hashmasks is a living digital art collectible created by over 70 artists globally. It is a collection of 16,384 unique digital portraits. Brought to you by Suum Cuique Labs from Zug, Switzerland.",
            attributes = attributes,
            image = imageUrl,
            imagePreview = null,
            imageBig = null,
            animationUrl = null,
            rawJsonContent = null
        )
    }
}
