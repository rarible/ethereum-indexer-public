package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.LoggingReducer
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.misc.combineIntoChain
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.token.reduce.status.EventStatusTokenReducer
import com.rarible.protocol.nft.core.service.token.reduce.status.TokenDeleteReducer
import org.springframework.stereotype.Component

@Component
class TokenReducer(
    eventStatusTokenReducer: EventStatusTokenReducer,
    tokenMetricReducer: TokenMetricReducer
) : Reducer<TokenEvent, Token> {

    private val eventStatusTokenReducer = combineIntoChain(
        LoggingReducer(),
        tokenMetricReducer,
        eventStatusTokenReducer,
        TokenDeleteReducer()
    )

    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent,
            is TokenEvent.TokenChangeOwnershipEvent,
            is TokenEvent.TokenPauseEvent -> {
                eventStatusTokenReducer.reduce(entity, event)
            }
        }
    }
}
