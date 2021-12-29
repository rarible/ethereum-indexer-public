package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component

@Component
class CompositeFullReduceService(
    entityService: CompositeUpdateService,
    entityIdService: CompositeEntityIdService,
    templateProvider: CompositeTemplateProvider,
    reducer: CompositeReducer
) : StreamFullReduceService<ItemId, CompositeEvent, CompositeEntity>(
    entityService = entityService,
    entityIdService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
)
