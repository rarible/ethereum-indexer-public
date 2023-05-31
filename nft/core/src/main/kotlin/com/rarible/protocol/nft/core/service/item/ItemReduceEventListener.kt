package com.rarible.protocol.nft.core.service.item

import com.rarible.protocol.nft.core.converters.dto.ItemEventDtoConverter
import com.rarible.protocol.nft.core.converters.dto.OwnershipEventDtoFromOwnershipConverter
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import kotlinx.coroutines.reactor.mono
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ItemReduceEventListener(
    private val publisher: ProtocolNftEventPublisher
) {

    fun onItemChanged(item: Item, event: ItemEvent? = null): Mono<Void> = mono {
        val eventDto = ItemEventDtoConverter.convert(item, event)
        publisher.publish(eventDto)
    }.then()

    fun onOwnershipChanged(ownership: Ownership, event: OwnershipEvent? = null): Mono<Void> = mono {
        val eventDto = OwnershipEventDtoFromOwnershipConverter.convert(ownership, event)
        publisher.publish(eventDto)
    }.then()

}
