package com.rarible.protocol.nft.core.service.token.reduce

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.misc.combineIntoChain
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.LoggingReducer
import com.rarible.protocol.nft.core.service.token.reduce.status.EventStatusTokenReducer
import com.rarible.protocol.nft.core.service.token.reduce.status.TokenDeleteReducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.stereotype.Component

@Component
@ExperimentalCoroutinesApi
class TokenReducer(
    eventStatusTokenReducer: EventStatusTokenReducer,
) : Reducer<TokenEvent, Token> {

    private val eventStatusTokenReducer = combineIntoChain(
        LoggingReducer(),
        eventStatusTokenReducer,
        TokenDeleteReducer()
    )

    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent,
            is TokenEvent.TokenChangeOwnershipEvent -> {
                eventStatusTokenReducer.reduce(entity, event)
            }
        }
    }
}
