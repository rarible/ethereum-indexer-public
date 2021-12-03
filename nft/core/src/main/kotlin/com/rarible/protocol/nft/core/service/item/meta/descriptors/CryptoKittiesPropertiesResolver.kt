package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.client.WebClientHelper
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import scalether.domain.Address

@Component
@CaptureSpan(type = META_CAPTURE_SPAN_TYPE)
class CryptoKittiesPropertiesResolver : ItemPropertiesResolver {

    private val client = WebClient.builder()
        .clientConnector(WebClientHelper.createConnector(10000, 10000, true))
        .build()
    private val mapper = ObjectMapper()

    override val name get() = "CryptoKitties"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != CRYPTO_KITTIES_ADDRESS) {
            return null
        }
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "get CryptoKitties properties ${itemId.tokenId.value}")

            client.get().uri("$CK_URL/${itemId.tokenId.value}")
                .retrieve()
                .bodyToMono<String>()
                .map {
                    val node = mapper.readTree(it) as ObjectNode
                    ItemProperties(
                        name = node.path("name").asText(),
                        description = node.path("bio")?.asText(),
                        image = node.path("image_url_cdn").asText(),
                        imagePreview = null,
                        imageBig = null,
                        animationUrl = null,
                        attributes = node.withArray("enhanced_cattributes")
                            .map { attr -> ItemAttribute(attr.path("type").asText(), attr.path("description").asText()) },
                        rawJsonContent = null
                    )
                }
        }.awaitFirstOrNull()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CryptoKittiesPropertiesResolver::class.java)
        private const val CK_URL = "https://api.cryptokitties.co/kitties"
        val CRYPTO_KITTIES_ADDRESS: Address = Address.apply("0x06012c8cf97bead5deae237070f9587f8e7a266d")
    }
}
