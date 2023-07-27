package com.rarible.protocol.erc20.core.service.reduce.reversed

import com.rarible.blockchain.scanner.ethereum.reduce.RevertedEntityChainReducer
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.service.reduce.Erc20CalculatedFieldsReducer
import com.rarible.protocol.erc20.core.service.reduce.Erc20RevertEventApplyPolicy
import org.springframework.stereotype.Component

@Component
class ReversedChainErc20BalanceReducer(
    erc20RevertEventApplyPolicy: Erc20RevertEventApplyPolicy,
    reversedValueErc20BalanceReducer: ReversedValueErc20BalanceReducer,
    erc20CalculatedFieldsReducer: Erc20CalculatedFieldsReducer
) : RevertedEntityChainReducer<BalanceId, Erc20Event, Erc20Balance>(
    erc20RevertEventApplyPolicy,
    reversedValueErc20BalanceReducer,
    erc20CalculatedFieldsReducer
)
