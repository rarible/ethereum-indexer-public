package com.rarible.protocol.nft.core.service.token.reduce.pending

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.token.reduce.forward.ForwardTokenReducer
import org.springframework.stereotype.Component

@Component
class PendingTokenReducer : Reducer<TokenEvent, Token> {
    private val delegate = ForwardTokenReducer()

    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent -> {
                val reducedEntity = delegate.reduce(entity, event)
                reducedEntity.copy(status = maxOf(ContractStatus.PENDING, entity.status))
            }
            is TokenEvent.TokenChangeOwnershipEvent -> {
                entity
            }
        }
    }
}
