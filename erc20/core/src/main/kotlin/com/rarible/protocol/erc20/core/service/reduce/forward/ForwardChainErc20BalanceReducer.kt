package com.rarible.protocol.erc20.core.service.reduce.forward

import com.rarible.blockchain.scanner.ethereum.reduce.EntityChainReducer
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.service.reduce.Erc20ConfirmEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ForwardChainErc20BalanceReducer(
    erc20ConfirmEventApplyPolicy: Erc20ConfirmEventApplyPolicy,
    forwardValueErc20BalanceReducer: ForwardValueErc20BalanceReducer
) : EntityChainReducer<BalanceId, Erc20Event, Erc20Balance>(
    erc20ConfirmEventApplyPolicy,
    forwardValueErc20BalanceReducer
)
