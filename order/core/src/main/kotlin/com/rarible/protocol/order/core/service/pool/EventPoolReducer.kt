package com.rarible.protocol.order.core.service.pool

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.order.core.model.OnChainAmmOrder
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.PoolAnyNftOut
import com.rarible.protocol.order.core.model.PoolExchangeHistory
import com.rarible.protocol.order.core.model.PoolNftDeposit
import com.rarible.protocol.order.core.model.PoolNftWithdraw
import com.rarible.protocol.order.core.model.PoolTargetNftIn
import com.rarible.protocol.order.core.model.PoolTargetNftOut
import org.springframework.stereotype.Component

@Component
class EventPoolReducer : Reducer<PoolExchangeHistory, Order> {
    override suspend fun reduce(entity: Order, event: PoolExchangeHistory): Order {
        return when (event) {
            is OnChainAmmOrder -> {
                onOnChainAmmOrder(entity, event)
            }
            is PoolAnyNftOut,
            is PoolTargetNftIn,
            is PoolTargetNftOut,
            is PoolNftWithdraw,
            is PoolNftDeposit -> entity
        }
    }

    private fun onOnChainAmmOrder(entity: Order, event: OnChainAmmOrder): Order {
        return entity.copy(
            maker = event.maker,
            make = event.make,
            take = event.take,
            createdAt = event.date,
            lastUpdateAt = maxOf(entity.lastUpdateAt, event.date),
            platform = event.source.toPlatform(),
            data = event.data,
            hash = event.hash,
            makePrice = event.priceValue.takeIf { event.isSell() },
            takePrice = event.priceValue.takeIf { event.isBid() },
        )
    }
}