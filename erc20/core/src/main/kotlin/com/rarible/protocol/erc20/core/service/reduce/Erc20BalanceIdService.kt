package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.core.entity.reducer.service.EntityIdService
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import org.springframework.stereotype.Component

@Component
class Erc20BalanceIdService : EntityIdService<Erc20MarkedEvent, BalanceId> {

    override fun getEntityId(event: Erc20MarkedEvent): BalanceId {
        return BalanceId.parseId(event.event.entityId)
    }
}
