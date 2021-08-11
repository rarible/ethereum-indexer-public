package com.rarible.protocol.erc20.core.model

import com.rarible.core.common.nowMillis
import java.util.*

sealed class Erc20BalanceEvent(
    val entityId: String
) {
    val id = UUID.randomUUID().toString()
    val timestamp = nowMillis()
    abstract val type: Erc20BalanceEventType
}

data class Erc20UpdateEvent(
    val balance: Erc20Balance
) : Erc20BalanceEvent(balance.id.stringValue) {
    override val type: Erc20BalanceEventType = Erc20BalanceEventType.UPDATE
}

enum class Erc20BalanceEventType {
    UPDATE
}