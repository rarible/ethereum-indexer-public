package com.rarible.protocol.nft.core.service.item.meta.descriptors

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.cache.CacheDescriptor
import com.rarible.protocol.contracts.external.waifus.Waifus
import com.rarible.protocol.nft.core.model.ItemAttribute
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.span.SpanType
import org.apache.commons.lang3.time.DateUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.transaction.MonoTransactionSender

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "waifusion-descriptor")
class WaifusionCacheDescriptor(
    sender: MonoTransactionSender,
    @Value("\${api.waifusion.address}") waifusionAddress: String,
    @Value("\${api.waifusion.cache-timeout}") private val cacheTimeout: Long,
    @Value("\${api.waifusion.ipfs-url-prefix}") private val waifusionIpfsUrlPrefix: String
) : CacheDescriptor<ItemProperties> {

    private val waifusion = Waifus(Address.apply(waifusionAddress), sender)
    final val token = "waifusion"
    override val collection: String = "cache_$token"

    override fun getMaxAge(value: ItemProperties?): Long = if (value == null) {
        DateUtils.MILLIS_PER_HOUR
    } else {
        cacheTimeout
    }

    override fun get(id: String): Mono<ItemProperties> {
        return waifusion.tokenNameByIndex(id.toBigInteger()).call()
            .flatMap { tuple ->
                val name = if (tuple.isNullOrEmpty()) "Waifu #$id" else tuple
                waifusion.ownerOf(id.toBigInteger()).call()
                    .map { ownerAddress ->
                        val attributes = listOf(
                            ItemAttribute("token", token),
                            ItemAttribute("owner", ownerAddress.toString())
                        )
                        ItemProperties(
                            name = name,
                            description = "Waifusion is a digital Waifu collection. There are 16,384 guaranteed-unique Waifusion NFTs. They’re just like you; a beautiful work of art, but 2-D and therefore, superior, Anon-kun.",
                            image = "$waifusionIpfsUrlPrefix/$id.png",
                            attributes = attributes,
                            imagePreview = null,
                            imageBig = null,
                            animationUrl = null
                        )
                    }
            }
    }
}
