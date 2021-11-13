package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.cache.CacheDescriptor
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.service.RoyaltyService
import kotlinx.coroutines.reactor.mono
import org.apache.commons.lang3.time.DateUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.EXT, subtype = "royalty")
class RoyaltyCacheDescriptor(
    private val royaltyService: RoyaltyService,
    @Value("\${api.royalty.cache-timeout}") private val cacheTimeout: Long
) : CacheDescriptor<List<Part>> {
    override val collection: String = "cache_royalty"

    override fun getMaxAge(value: List<Part>?): Long =
        if (value == null) 10 * DateUtils.MILLIS_PER_MINUTE else cacheTimeout

    override fun get(id: String): Mono<List<Part>> = mono {
        val itemId = ItemId.parseId(id)
        royaltyService.getByToken(itemId.token, itemId.tokenId)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RoyaltyCacheDescriptor::class.java)
    }
}
