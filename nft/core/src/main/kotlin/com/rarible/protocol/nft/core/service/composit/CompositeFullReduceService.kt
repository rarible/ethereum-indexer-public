package com.rarible.protocol.nft.core.service.composit

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.nft.core.model.CompositeEntity
import com.rarible.protocol.nft.core.model.CompositeEvent
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.stereotype.Component

sealed class CompositeFullReduceService(
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

@Component
class SilentCompositeFullReduceService(
    entityService: SilentCompositeUpdateService,
    entityIdService: CompositeEntityIdService,
    templateProvider: CompositeTemplateProvider,
    reducer: CompositeReducer
) : CompositeFullReduceService(
    entityService = entityService,
    entityIdService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
)

@Component
class VerboseCompositeFullReduceService(
    entityService: VerboseCompositeUpdateService,
    entityIdService: CompositeEntityIdService,
    templateProvider: CompositeTemplateProvider,
    reducer: CompositeReducer
) : CompositeFullReduceService(
    entityService = entityService,
    entityIdService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
)
