package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.contracts.external.loot.LootMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.transaction.MonoTransactionSender

@Component
class LootCacheDescriptor(
    @Value("\${api.loot.cache-timeout}") private val cacheTimeout: Long,
    val sender: MonoTransactionSender,
    val mapper: ObjectMapper,
    val ipfsService: IpfsService
) : CacheDescriptor<ItemProperties> {

    val contract = LootMeta(ItemPropertiesService.LOOT_ADDRESS, sender)

    override val collection: String = "cache_loot"

    override fun getMaxAge(value: ItemProperties?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            cacheTimeout
        }

    override fun get(id: String): Mono<ItemProperties> {
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "get properties $id")

            val tokenId = id.parseTokenId().second
            mono {
                val ll = listOf(
                    contract.getChest(tokenId),
                    contract.getFoot(tokenId),
                    contract.getHand(tokenId),
                    contract.getHead(tokenId),
                    contract.getNeck(tokenId),
                    contract.getRing(tokenId),
                    contract.getWaist(tokenId),
                    contract.getWeapon(tokenId)
                ).map { async { it.call().awaitFirstOrNull() ?: "" } }.awaitAll()
                val def = "chest foot hand head neck ring waist weapon".split(" ")
                val attrs = ll.mapIndexed { i, v -> ItemAttribute(def[i], v) }.toList()
                val node = mapper.readTree(base64MimeToString(contract.tokenURI(tokenId).call().awaitSingle())) as ObjectNode
                val imageUrl = node.getText("image")?.let {
                    val hash = ipfsService.upload("image.svg", base64MimeToBytes(it), "image/svg+xml")
                    ipfsService.url(hash)
                } ?: ""
                ItemProperties(
                    name = node.getText("name")  ?: DEFAULT_TITLE,
                    description = node.getText("description"),
                    image = imageUrl,
                    imagePreview = null,
                    animationUrl = imageUrl,
                    imageBig = null,
                    attributes = attrs
                )
            }
        }
    }

    companion object {
        const val DEFAULT_TITLE = "Untitled"
        val logger: Logger = LoggerFactory.getLogger(LootCacheDescriptor::class.java)
    }

}
