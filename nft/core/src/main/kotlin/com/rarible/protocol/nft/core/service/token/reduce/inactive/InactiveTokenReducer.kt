package com.rarible.protocol.nft.core.service.token.reduce.inactive

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import org.springframework.stereotype.Component

@Component
class InactiveTokenReducer : Reducer<TokenEvent, Token> {

    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent -> {
                entity.copy(status = maxOf(ContractStatus.ERROR, entity.status))
            }
            is TokenEvent.TokenChangeOwnershipEvent -> {
                entity
            }
        }
    }
}
