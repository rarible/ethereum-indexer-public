package com.rarible.protocol.nft.core.service.ownership.reduce

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.model.OwnershipId
import org.springframework.stereotype.Component

@Component
class OwnershipIdService : EntityIdService<OwnershipEvent, OwnershipId> {
    override fun getEntityId(event: OwnershipEvent): OwnershipId {
        return OwnershipId.parseId(event.entityId)
    }
}
