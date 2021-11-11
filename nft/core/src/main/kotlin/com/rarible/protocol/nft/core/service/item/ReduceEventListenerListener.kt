package com.rarible.protocol.nft.core.service.item

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.nft.core.model.*
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.span.SpanType
import kotlinx.coroutines.reactor.mono
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@CaptureSpan(type = SpanType.SERVICE, subtype = "reduce-event-listener")
class ReduceEventListenerListener(
    private val publisher: ProtocolNftEventPublisher,
    private val conversionService: ConversionService
) {
    fun onItemChanged(item: Item): Mono<Void> = mono {
        val eventDto = conversionService.convert<NftItemEventDto>(ExtendedItem(item, ItemMeta.EMPTY))
        publisher.publishInternalItem(eventDto)
    }.then()

    fun onOwnershipChanged(ownership: Ownership): Mono<Void> = mono {
        val eventDto = conversionService.convert<NftOwnershipEventDto>(ownership)
        publisher.publish(eventDto)
    }.then()

    fun onOwnershipDeleted(ownershipId: OwnershipId): Mono<Void> = mono {
        val eventDto = conversionService.convert<NftOwnershipEventDto>(ownershipId)
        publisher.publish(eventDto)
    }.then()
}
