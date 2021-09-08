package com.rarible.protocol.nft.api.service.item.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.contracts.external.loot.LootMeta
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
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
    val mapper: ObjectMapper
) : CacheDescriptor<ItemProperties> {

    val contract = LootMeta(ItemPropertiesService.LOOT_ADDRESS, sender)

    override val collection: String = "cache_properties"

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
                val attrs = mutableListOf<ItemAttribute>()
                contract.getChest(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("chest", it)) }
                contract.getFoot(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("foot", it)) }
                contract.getHand(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("hand", it)) }
                contract.getHead(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("head", it)) }
                contract.getNeck(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("neck", it)) }
                contract.getRing(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("ring", it)) }
                contract.getWaist(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("waist", it)) }
                contract.getWeapon(tokenId).call().awaitFirstOrNull()?.let { attrs.add(ItemAttribute("weapon", it)) }
                val node = mapper.readTree(base64ToString(contract.tokenURI(tokenId).call().awaitSingle())) as ObjectNode
                ItemProperties(
                    name = node.getText("name")  ?: DEFAULT_TITLE,
                    description = node.getText("description"),
                    image = "",
                    imagePreview = null,
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
