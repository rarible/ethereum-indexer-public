package com.rarible.protocol.nft.core.service.item

import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.nft.core.converters.dto.ItemEventDtoConverter
import com.rarible.protocol.nft.core.converters.dto.OwnershipEventDtoConverter
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ItemReduceEventListener(
    private val publisher: ProtocolNftEventPublisher
) {

    fun onItemChanged(item: Item, eventTimeMarks: EventTimeMarks): Mono<Void> = mono {
        val eventDto = ItemEventDtoConverter.convert(item, eventTimeMarks)
        publisher.publish(eventDto)
    }.then()

    fun onOwnershipChanged(ownership: Ownership, eventTimeMarks: EventTimeMarks): Mono<Void> = mono {
        val eventDto = OwnershipEventDtoConverter.convert(ownership, eventTimeMarks)
        publisher.publish(eventDto)
    }.then()
}
