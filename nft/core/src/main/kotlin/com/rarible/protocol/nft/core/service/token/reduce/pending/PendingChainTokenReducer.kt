package com.rarible.protocol.nft.core.service.token.reduce.pending

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.token.reduce.TokenPendingEventApplyPolicy
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class PendingChainTokenReducer(
    tokenPendingEventApplyPolicy: TokenPendingEventApplyPolicy
) : EntityChainReducer<Address, TokenEvent, Token>(
    tokenPendingEventApplyPolicy
)
