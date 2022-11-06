package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component

@Component
class Erc20BalanceIdService : EntityIdService<Erc20Event, BalanceId> {
    override fun getEntityId(event: Erc20Event): BalanceId {
        return BalanceId.parseId(event.entityId)
    }
}
