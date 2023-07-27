package com.rarible.protocol.erc20.core.event

import com.rarible.protocol.erc20.core.model.Erc20BalanceEvent

interface Erc20BalanceEventListener {

    suspend fun onUpdate(event: Erc20BalanceEvent)
}
