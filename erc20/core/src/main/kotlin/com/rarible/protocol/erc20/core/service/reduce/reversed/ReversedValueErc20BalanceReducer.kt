package com.rarible.protocol.erc20.core.service.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.service.reduce.forward.ForwardValueErc20BalanceReducer
import org.springframework.stereotype.Component

@Component
class ReversedValueErc20BalanceReducer(
    private val forwardValueErc20BalanceReducer: ForwardValueErc20BalanceReducer
) : Reducer<Erc20Event, Erc20Balance> {
    override suspend fun reduce(entity: Erc20Balance, event: Erc20Event): Erc20Balance =
        forwardValueErc20BalanceReducer.reduce(entity, event.invert())
}
