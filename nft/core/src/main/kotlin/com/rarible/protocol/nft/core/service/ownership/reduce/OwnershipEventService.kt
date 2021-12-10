package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityEventService
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component

@Component
class OwnershipEventService : EntityEventService<OwnershipEvent, OwnershipId> {
    override fun getEntityId(event: OwnershipEvent): OwnershipId {
        return OwnershipId.parseId(event.entityId)
    }
}
