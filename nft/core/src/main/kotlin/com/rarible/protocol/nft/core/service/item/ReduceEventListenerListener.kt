package com.rarible.protocol.nft.core.service.item

import com.rarible.core.common.convert
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.nft.core.converters.dto.OwnershipEventDtoFromOwnershipConverter
import com.rarible.protocol.nft.core.model.ExtendedItem
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.producer.ProtocolNftEventPublisher
import com.rarible.protocol.nft.core.service.item.meta.ItemMetaService
import com.rarible.protocol.nft.core.service.item.meta.ItemUpdateEventAssertQueue
import kotlinx.coroutines.reactor.mono
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ReduceEventListenerListener(
    private val publisher: ProtocolNftEventPublisher,
    private val conversionService: ConversionService,
    private val itemMetaService: ItemMetaService,
    private val itemUpdateEventAssertQueue: ItemUpdateEventAssertQueue,
    private val converter: OwnershipEventDtoFromOwnershipConverter
) {

    fun onItemChanged(item: Item): Mono<Void> = mono {
        val availableMeta = itemMetaService.getAvailable(item.id)
        if (itemMetaService.isMetaInitiallyLoadedOrFailed(item.id)) {
            // Return with available meta as is (even if it is null).
            publisher.publish(conversionService.convert<NftItemEventDto>(ExtendedItem(item, availableMeta)))
        } else {
            itemMetaService.scheduleMetaUpdate(item.id)
            /*
             * Ideally, the [ItemMetaCacheLoaderEventListener] will send the item update notification
             * when the item meta gets loaded.
             * But since the meta loading infrastructure is not stable yet, let's re-assure
             * that we send the items event. Logging will help us detect missed out items and bugs.
             */
            itemUpdateEventAssertQueue.sendItemUpdateEventAfter(item.id, ItemUpdateEventAssertQueue.delay)
        }
    }.then()

    fun onOwnershipChanged(ownership: Ownership): Mono<Void> = mono {
        val eventDto = converter.convert(ownership)
        publisher.publish(eventDto)
    }.then()

    fun onOwnershipDeleted(ownership: Ownership): Mono<Void> = mono {
        val deletedDto = converter.convertToDeleteEvent(ownership)
        publisher.publish(deletedDto)
    }.then()

}
