package com.rarible.protocol.erc20.core.event

import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent

interface Erc20BalanceEventListener {

    suspend fun onUpdate(event: Erc20UpdateEvent)

}