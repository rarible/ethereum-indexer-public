package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.rarible.core.cache.CacheDescriptor
import com.rarible.core.client.WebClientHelper
import com.rarible.core.logging.LoggingUtils
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.service.item.meta.parseTokenId
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Component
class RoyaltyCacheDescriptor(
    @Value("\${api.kitties.cache-timeout}") private val cacheTimeout: Long
) : CacheDescriptor<List<Part>> {
    override val collection: String = "cache_royalty"

    override fun getMaxAge(value: List<Part>?): Long =
        if (value == null) {
            DateUtils.MILLIS_PER_HOUR
        } else {
            cacheTimeout
        }

    override fun get(id: String): Mono<List<Part>> {

        val tokenId = id.parseTokenId().second

        return Mono.empty()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RoyaltyCacheDescriptor::class.java)
    }
}
