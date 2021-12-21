package com.rarible.protocol.nft.core.service.token.reduce.forward

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.EntityChainReducer
import com.rarible.protocol.nft.core.service.token.reduce.TokenConfirmEventApplyPolicy
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ForwardChainTokenReducer(
    tokenConfirmEventApplyPolicy: TokenConfirmEventApplyPolicy
) : EntityChainReducer<Address, TokenEvent, Token>(
    tokenConfirmEventApplyPolicy
)
