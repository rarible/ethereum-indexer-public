package com.rarible.protocol.erc20.core.service.reduce.reversed

import com.rarible.blockchain.scanner.ethereum.reduce.RevertCompactEventsReducer
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component

// TODO: Full support [CompactEventsReducer]
@Component
class RevertBalanceCompactEventsReducer : RevertCompactEventsReducer<BalanceId, Erc20Event, Erc20Balance>() {
    override fun merge(reverted: Erc20Event, compact: Erc20Event): Erc20Event {
        return reverted.withValue(compact.value)
    }
}
