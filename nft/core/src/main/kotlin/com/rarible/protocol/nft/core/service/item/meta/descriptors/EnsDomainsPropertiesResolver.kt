package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.client.WebClientHelper
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.ItemPropertiesResolver
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import scalether.domain.Address


@Component
@CaptureSpan(type = ITEM_META_CAPTURE_SPAN_TYPE)
class EnsDomainsPropertiesResolver: ItemPropertiesResolver {

    companion object {
        private val logger = LoggerFactory.getLogger(EnsDomainsPropertiesResolver::class.java)
        private const val URL = "https://metadata.ens.domains/"
        private const val NETWORK = "mainnet"
        val ENS_DOMAINS_ADDRESS: Address = Address.apply("0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85")
    }

    private val client = WebClient.builder()
        .clientConnector(WebClientHelper.createConnector(10000, 10000, true))
        .build()
    private val mapper = ObjectMapper()

    override val name get() = "EnsDomains"

    override suspend fun resolve(itemId: ItemId): ItemProperties? {
        if (itemId.token != ENS_DOMAINS_ADDRESS) {
            return null
        }
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "get EnsDomains properties ${itemId.tokenId.value}")

            client.get().uri("${URL}/${NETWORK}/${ENS_DOMAINS_ADDRESS}/${itemId.tokenId.value}")
                .retrieve()
                .bodyToMono<String>()
                .map {
                    val node = mapper.readTree(it) as ObjectNode
                    ItemProperties(
                        name = node.path("name").asText(),
                        description = node.path("description").asText(),
                        image = node.path("image_url").asText(),
                        imagePreview = null,
                        imageBig = null,
                        animationUrl = null,
                        attributes = node.parseAttributes(milliTimestamps = true),
                        rawJsonContent = node.toString()
                    )
                }
        }.awaitFirstOrNull()
    }
}
