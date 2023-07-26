package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.protocol.erc20.core.model.Erc20Event

object Erc20EventInverter {
    fun invert(event: Erc20Event.Erc20IncomeTransferEvent): Erc20Event = Erc20Event.Erc20OutcomeTransferEvent(
        event.entityId, event.log, event.owner, event.value, event.token, event.date
    )

    fun invert(event: Erc20Event.Erc20OutcomeTransferEvent): Erc20Event = Erc20Event.Erc20IncomeTransferEvent(
        event.entityId, event.log, event.owner, event.value, event.token, event.date
    )

    fun invert(event: Erc20Event.Erc20DepositEvent): Erc20Event = Erc20Event.Erc20WithdrawalEvent(
        event.entityId, event.log, event.owner, event.value, event.token, event.date
    )

    fun invert(event: Erc20Event.Erc20WithdrawalEvent): Erc20Event = Erc20Event.Erc20DepositEvent(
        event.entityId, event.log, event.owner, event.value, event.token, event.date
    )
}
