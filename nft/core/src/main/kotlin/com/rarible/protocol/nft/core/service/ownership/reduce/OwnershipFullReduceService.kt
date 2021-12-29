package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.StreamFullReduceService
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component

@Component
class OwnershipFullReduceService(
    entityService: OwnershipUpdateService,
    entityIdService: OwnershipIdService,
    templateProvider: OwnershipTemplateProvider,
    reducer: OwnershipReducer
) : StreamFullReduceService<OwnershipId, OwnershipEvent, Ownership>(
    entityService = entityService,
    entityIdService = entityIdService,
    templateProvider = templateProvider,
    reducer = reducer
)
