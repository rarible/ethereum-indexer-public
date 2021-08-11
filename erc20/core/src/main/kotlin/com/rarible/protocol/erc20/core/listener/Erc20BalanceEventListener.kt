package com.rarible.protocol.erc20.core.listener

import com.rarible.protocol.erc20.core.model.Erc20UpdateEvent

interface Erc20BalanceEventListener {

    suspend fun onUpdate(event: Erc20UpdateEvent)

}