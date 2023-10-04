package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.contracts.external.loot.LootMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.getText
import com.rarible.protocol.nft.core.service.item.meta.logMetaLoading
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.JsonPropertiesParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
class LootPropertiesResolver(
    sender: MonoTransactionSender,
    val mapper: ObjectMapper
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

        @Suppress("BlockingMethodInNonBlockingContext")
        val node = JsonPropertiesParser.parse(itemId, tokenUri)
        check(node != null)
        val imageUrl = node.getText("image")
        val name = node.getText("name") ?: return null
        return ItemProperties(
            name = name,
            description = node.getText("description"),
            attributes = attrs,
            rawJsonContent = null,
            content = ContentBuilder.getItemMetaContent(
                imageOriginal = imageUrl,
                videoOriginal = imageUrl
            )
        )
    }

    companion object {
        val LOOT_ADDRESS: Address = Address.apply("0xff9c1b15b16263c61d017ee9f65c50e4ae0113d7")
    }
}
