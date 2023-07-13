package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.EventStatusReducer
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.service.reduce.forward.ForwardChainErc20BalanceReducer
import com.rarible.protocol.erc20.core.service.reduce.reversed.ReversedChainErc20BalanceReducer
import com.rarible.protocol.erc20.core.service.reduce.reversed.RevertBalanceCompactEventsReducer
import org.springframework.stereotype.Component

@Component
class EventStatusErc20BalanceReducer(
    forwardChainErc20BalanceReducer: ForwardChainErc20BalanceReducer,
    reversedChainErc20BalanceReducer: ReversedChainErc20BalanceReducer,
    revertBalanceCompactEventsReducer: RevertBalanceCompactEventsReducer
) : EventStatusReducer<BalanceId, Erc20Event, Erc20Balance>(
    forwardChainErc20BalanceReducer,
    reversedChainErc20BalanceReducer,
    revertBalanceCompactEventsReducer,
)