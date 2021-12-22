package com.rarible.protocol.nft.core.service.token.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.model.TokenStandard
import org.springframework.stereotype.Component

@Component
class ForwardTokenReducer : Reducer<TokenEvent, Token> {

    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return when (event) {
            is TokenEvent.TokenCreateEvent -> {
                val (standard, features) = TokenStandard.CREATE_TOPIC_MAP[event.log.topic]
                    ?: (TokenStandard.NONE to emptySet())

                entity.copy(
                    owner = event.owner,
                    name = event.name,
                    symbol = event.symbol,
                    status = ContractStatus.CONFIRMED,
                    features = features,
                    standard = standard,
                    isRaribleContract = true,
                )
            }
            is TokenEvent.TokenChangeOwnershipEvent -> {
                entity.copy(
                    owner = event.owner
                )
            }
        }
    }
}
