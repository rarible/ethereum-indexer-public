package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.EntityService
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

@Component
class ConsistencyCorrectorFactory(
    private val reducer: CompositeReducer,
    private val itemEventConverter: ItemEventConverter,
) {
    fun wrap(delegate: EntityService<ItemId, CompositeEntity>): EntityService<ItemId, CompositeEntity> {
        return ConsistencyCorrectorEntityService(delegate, reducer, itemEventConverter)
    }
}