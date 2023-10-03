package com.rarible.protocol.nft.core.service.token.reduce.reverted

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.TokenFlag
import org.springframework.stereotype.Component

@Component
class RevertedTokenReducer : Reducer<TokenEvent, Token> {
    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent -> {
                entity.copy(status = ContractStatus.ERROR)
            }

            is TokenEvent.TokenChangeOwnershipEvent -> {
                entity.copy(owner = event.previousOwner)
            }

            is TokenEvent.TokenPauseEvent -> {
                entity.copy(
                    flags = entity.flags.plus(Pair(TokenFlag.PAUSED, (!event.paused).toString()))
                )
            }
        }
    }
}
