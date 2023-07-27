package com.rarible.protocol.nft.core.service.token.reduce.status

import com.rarible.blockchain.scanner.ethereum.reduce.EventStatusReducer
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.token.reduce.forward.ForwardChainTokenReducer
import com.rarible.protocol.nft.core.service.token.reduce.reverted.ReversedChainTokenReducer
import com.rarible.protocol.nft.core.service.token.reduce.reverted.RevertTokenCompactEventsReducer
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class EventStatusTokenReducer(
    forwardChainTokenReducer: ForwardChainTokenReducer,
    reversedChainTokenReducer: ReversedChainTokenReducer,
    revertTokenCompactEventsReducer: RevertTokenCompactEventsReducer
) : EventStatusReducer<Address, TokenEvent, Token>(
    forwardChainTokenReducer,
    reversedChainTokenReducer,
    revertTokenCompactEventsReducer,
)
