package com.rarible.protocol.nft.core.service.token.reduce.status

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent

class TokenDeleteReducer : Reducer<TokenEvent, Token> {
    override suspend fun reduce(entity: Token, event: TokenEvent): Token {
        return entity.copy(deleted = entity.status == ContractStatus.ERROR)
    }
}
