package com.rarible.protocol.erc20.core.service.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component

@Component
class ForwardValueErc20BalanceReducer : Reducer<Erc20Event, Erc20Balance> {
    override suspend fun reduce(entity: Erc20Balance, event: Erc20Event): Erc20Balance {

        val currentBalance = entity.balance
        return when (event) {
            is Erc20Event.Erc20IncomeTransferEvent -> entity.copy(balance = currentBalance + event.value)
            is Erc20Event.Erc20OutcomeTransferEvent -> entity.copy(balance = currentBalance - event.value)
            is Erc20Event.Erc20DepositEvent -> entity.copy(balance = currentBalance + event.value)
            is Erc20Event.Erc20WithdrawalEvent -> entity.copy(balance = currentBalance - event.value)
            is Erc20Event.Erc20TokenApprovalEvent -> entity

        }.withBlockNumber(blockNumber = event.log.blockNumber)
    }
}
