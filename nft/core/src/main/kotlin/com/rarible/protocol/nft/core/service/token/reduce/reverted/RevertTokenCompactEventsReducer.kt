package com.rarible.protocol.nft.core.service.token.reduce.reverted

import com.rarible.blockchain.scanner.ethereum.reduce.RevertCompactEventsReducer
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class RevertTokenCompactEventsReducer : RevertCompactEventsReducer<Address, TokenEvent, Token>() {

    override fun merge(reverted: TokenEvent, compact: TokenEvent): TokenEvent {
        return reverted
    }
}
