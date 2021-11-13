package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.cache.CacheDescriptor
import com.rarible.protocol.contracts.external.hashmasks.Hashmasks
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import org.apache.commons.lang3.time.DateUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@CaptureSpan(type = SpanType.EXT, subtype = "meta")
class HashmasksCacheDescriptor(
    sender: MonoTransactionSender,
    @Value("\${api.hashmasks.address}") hashmasksAddress: String,
    @Value("\${api.hashmasks.cache-timeout}") private val cacheTimeout: Long
) : CacheDescriptor<ItemProperties> {
    private val hashmasks = Hashmasks(Address.apply(hashmasksAddress), sender)
    final val token = "hashmasks"
    override val collection: String = "cache_$token"

    override fun getMaxAge(value: ItemProperties?): Long = if (value == null) {
        DateUtils.MILLIS_PER_HOUR
    } else {
        cacheTimeout
    }

    override fun get(id: String): Mono<ItemProperties> {
        return hashmasks.tokenNameByIndex(id.toBigInteger()).call()
            .flatMap { tuple ->
                val name = if (tuple.isNullOrEmpty()) "Hashmask #$id" else tuple
                hashmasks.ownerOf(id.toBigInteger()).call()
                    .map { ownerAddress ->
                        val attributes = listOf(
                            ItemAttribute("token", token),
                            ItemAttribute("owner", ownerAddress.toString())
                        )
                        ItemProperties(
                            name = name,
                            description = "Hashmasks is a living digital art collectible created by over 70 artists globally. It is a collection of 16,384 unique digital portraits. Brought to you by Suum Cuique Labs from Zug, Switzerland.",
                            attributes = attributes,
                            image = null,
                            imagePreview = null,
                            imageBig = null,
                            animationUrl = null
                        )
                    }
            }
    }
}
