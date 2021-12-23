package com.rarible.protocol.nft.core.service.token.reduce.inactive

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.token.reduce.TokenInactiveEventApplyPolicy
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class InactiveChainTokenReducer(
    tokenInactiveEventApplyPolicy: TokenInactiveEventApplyPolicy
) : EntityChainReducer<Address, TokenEvent, Token>(
    tokenInactiveEventApplyPolicy
)
