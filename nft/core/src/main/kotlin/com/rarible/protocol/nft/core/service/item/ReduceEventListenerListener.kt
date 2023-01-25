package com.rarible.protocol.nft.core.service.item

import com.rarible.protocol.nft.core.converters.dto.ItemEventDtoConverter
import com.rarible.protocol.nft.core.converters.dto.OwnershipEventDtoFromOwnershipConverter
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ReduceEventListenerListener(
    private val publisher: ProtocolNftEventPublisher
) {

    fun onItemChanged(item: Item): Mono<Void> = mono {
        val eventDto = ItemEventDtoConverter.convert(item)
        publisher.publish(eventDto)
    }.then()

    fun onOwnershipChanged(ownership: Ownership): Mono<Void> = mono {
        val eventDto = OwnershipEventDtoFromOwnershipConverter.convert(ownership)
        publisher.publish(eventDto)
    }.then()

    fun onOwnershipDeleted(ownership: Ownership): Mono<Void> = mono {
        val deletedDto = OwnershipEventDtoFromOwnershipConverter.convertToDeleteEvent(ownership)
        publisher.publish(deletedDto)
    }.then()

}
