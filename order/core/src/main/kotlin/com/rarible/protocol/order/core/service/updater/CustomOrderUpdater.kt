package com.rarible.protocol.order.core.service.updater

import com.rarible.protocol.order.core.model.MakeBalanceState
import com.rarible.protocol.order.core.model.Order

interface CustomOrderUpdater {

    suspend fun update(order: Order, makeBalanceState: MakeBalanceState? = null): Order

}