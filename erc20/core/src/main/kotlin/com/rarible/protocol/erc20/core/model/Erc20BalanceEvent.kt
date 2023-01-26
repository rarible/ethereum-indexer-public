package com.rarible.protocol.erc20.core.model

import java.util.*

sealed class Erc20BalanceEvent {

    abstract val event: Erc20Event?
    val id = UUID.randomUUID().toString()
    abstract val type: Erc20BalanceEventType
}

data class Erc20UpdateEvent(
    override val event: Erc20Event?,
    val balance: Erc20Balance
) : Erc20BalanceEvent() {

    override val type: Erc20BalanceEventType = Erc20BalanceEventType.UPDATE
}

enum class Erc20BalanceEventType {
    UPDATE
}