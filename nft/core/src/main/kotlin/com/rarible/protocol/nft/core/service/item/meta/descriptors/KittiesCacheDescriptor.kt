package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.client.WebClientHelper
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.service.item.meta.parseTokenId
import com.rarible.protocol.nft.core.span.SpanType
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "kitties-descriptor")
class KittiesCacheDescriptor(
    @Value("\${api.kitties.cache-timeout}") private val cacheTimeout: Long
) : CacheDescriptor<ItemProperties> {
    override val collection: String = "cache_properties"

    override fun getMaxAge(value: ItemProperties?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            cacheTimeout
        }

    private val client = WebClient.builder()
        .clientConnector(WebClientHelper.createConnector(10000, 10000, true))
        .build()
    private val mapper = ObjectMapper()

    override fun get(id: String): Mono<ItemProperties> {
        return LoggingUtils.withMarker { marker ->
            logger.info(marker, "get properties $id")

            val tokenId = id.parseTokenId().second

            client.get().uri("$CK_URL/$tokenId")
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
                        attributes = node.withArray("enhanced_cattributes")
                            .map { attr -> ItemAttribute(attr.path("type").asText(), attr.path("description").asText()) }
                    )
                }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(KittiesCacheDescriptor::class.java)
        const val CK_URL = "https://api.cryptokitties.co/kitties"
    }
}
