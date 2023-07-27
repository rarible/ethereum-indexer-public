package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.LoggingReducer
import com.rarible.blockchain.scanner.ethereum.reduce.combineIntoChain
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import org.springframework.stereotype.Component

@Component
class Erc20BalanceReducer(
    erc20BalanceMetricReducer: Erc20BalanceMetricReducer,
    eventStatusErc20BalanceReducer: EventStatusErc20BalanceReducer
) : Reducer<Erc20MarkedEvent, Erc20Balance> {

    private val eventStatusReducer = combineIntoChain(
        LoggingReducer(),
        erc20BalanceMetricReducer,
        eventStatusErc20BalanceReducer
    )

    override suspend fun reduce(entity: Erc20Balance, event: Erc20MarkedEvent): Erc20Balance =
        eventStatusReducer.reduce(entity, event.event)
}
