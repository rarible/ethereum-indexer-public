package com.rarible.protocol.nft.core.service.ownership.reduce.status

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent

class OwnershipCalculatedFieldReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return entity.withCalculatedFields()
    }
}
