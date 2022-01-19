package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.contracts.external.loot.LootMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.IpfsService
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class LootPropertiesResolver(
    sender: MonoTransactionSender,
    val mapper: ObjectMapper,
    val ipfsService: IpfsService
) : ItemPropertiesResolver {

    private val contract = LootMeta(LOOT_ADDRESS, sender)

    override val name get() = "Loot"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != LOOT_ADDRESS) {
            return null
        }
        logMetaLoading(itemId, "getting Loot properties")
        val tokenId = itemId.tokenId.value
        val ll = coroutineScope {
            listOf(
                contract.getChest(tokenId),
                contract.getFoot(tokenId),
                contract.getHand(tokenId),
                contract.getHead(tokenId),
                contract.getNeck(tokenId),
                contract.getRing(tokenId),
                contract.getWaist(tokenId),
                contract.getWeapon(tokenId)
            ).map { async { it.call().awaitFirstOrNull() ?: "" } }.awaitAll()
        }
        val def = "chest foot hand head neck ring waist weapon".split(" ")
        val attrs = ll.mapIndexed { i, v -> ItemAttribute(def[i], v) }.toList()
        val tokenUri = contract.tokenURI(tokenId).call().awaitSingle()
        check(tokenUri.startsWith(BASE_64_JSON_PREFIX))
        @Suppress("BlockingMethodInNonBlockingContext")
        val node = mapper.readTree(base64MimeToBytes(tokenUri.removePrefix(BASE_64_JSON_PREFIX))) as ObjectNode
        val imageUrl = node.getText("image")?.let {
            check(it.startsWith(BASE_64_SVG_PREFIX))
            val svgBytes = base64MimeToBytes(it.removePrefix(BASE_64_SVG_PREFIX))
            ipfsService.upload("image-${tokenId}.svg", svgBytes, "image/svg+xml")
        }
        val name = node.getText("name") ?: return null
        return ItemProperties(
            name = name,
            description = node.getText("description"),
            image = imageUrl,
            imagePreview = null,
            animationUrl = imageUrl,
            imageBig = null,
            attributes = attrs,
            rawJsonContent = null
        )
    }

    companion object {
        val LOOT_ADDRESS: Address = Address.apply("0xff9c1b15b16263c61d017ee9f65c50e4ae0113d7")
    }

}
