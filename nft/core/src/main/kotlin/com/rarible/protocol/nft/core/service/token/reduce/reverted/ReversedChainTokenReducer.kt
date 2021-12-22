package com.rarible.protocol.nft.core.service.token.reduce.reverted

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenEvent
import com.rarible.protocol.nft.core.service.RevertedEntityChainReducer
import com.rarible.protocol.nft.core.service.token.reduce.TokenRevertEventApplyPolicy
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReversedChainTokenReducer(
    tokenRevertEventApplyPolicy: TokenRevertEventApplyPolicy
) : RevertedEntityChainReducer<Address, TokenEvent, Token>(
    tokenRevertEventApplyPolicy,
    RevertedTokenReducer()
)
