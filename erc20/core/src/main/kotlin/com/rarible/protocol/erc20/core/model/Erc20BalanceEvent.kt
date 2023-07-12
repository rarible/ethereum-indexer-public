package com.rarible.protocol.erc20.core.model

import com.rarible.core.common.EventTimeMarks
import java.util.UUID

sealed class Erc20BalanceEvent {

    abstract val event: Erc20Event?
    val id = UUID.randomUUID().toString()
    abstract val type: Erc20BalanceEventType
    abstract val eventTimeMarks: EventTimeMarks?
}

data class Erc20UpdateEvent(
    override val event: Erc20Event?,
    override val eventTimeMarks: EventTimeMarks?,
    val balance: Erc20Balance

) : Erc20BalanceEvent() {

    override val type: Erc20BalanceEventType = Erc20BalanceEventType.UPDATE
}

data class Erc20AllowanceEvent(
    override val event: Erc20Event?,
    override val eventTimeMarks: EventTimeMarks?,
    val allowance: Erc20Allowance
): Erc20BalanceEvent() {
    override val type: Erc20BalanceEventType = Erc20BalanceEventType.ALLOWANCE
}

enum class Erc20BalanceEventType {
    UPDATE,
    ALLOWANCE,
}