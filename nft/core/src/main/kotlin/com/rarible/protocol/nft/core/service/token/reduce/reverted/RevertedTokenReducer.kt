package com.rarible.protocol.nft.core.service.token.reduce.reverted

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import org.springframework.stereotype.Component

@Component
class RevertedTokenReducer : Reducer<TokenEvent, Token> {
    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent -> {
                entity.copy(status = ContractStatus.ERROR, deleted = true)
            }
            is TokenEvent.TokenChangeOwnershipEvent -> {
                entity.copy(owner = event.previousOwner)
            }
        }
    }
}
