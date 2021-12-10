package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.TaskReduceService
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component

@Component
class OwnershipTaskReduceService(
    entityService: OwnershipUpdateService,
    entityEventService: OwnershipEventService,
    templateProvider: OwnershipTemplateProvider,
    reducer: OwnershipReducer
) : TaskReduceService<OwnershipId, OwnershipEvent, Ownership>(
    entityService = entityService,
    entityEventService = entityEventService,
    templateProvider = templateProvider,
    reducer = reducer
)
