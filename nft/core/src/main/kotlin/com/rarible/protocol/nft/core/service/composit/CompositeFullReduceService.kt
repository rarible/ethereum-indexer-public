package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component

@Component
class CompositeFullReduceService(
    entityService: CompositeUpdateService,
    entityEventService: CompositeEventService,
    templateProvider: CompositeTemplateProvider,
    reducer: CompositeReducer
) : StreamFullReduceService<CompositeEntityId, CompositeEvent, CompositeEntity>(
    entityService = entityService,
    entityEventService = entityEventService,
    templateProvider = templateProvider,
    reducer = reducer
)
