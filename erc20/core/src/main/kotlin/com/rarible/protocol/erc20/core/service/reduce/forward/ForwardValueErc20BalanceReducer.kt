package com.rarible.protocol.erc20.core.service.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ForwardValueErc20BalanceReducer : Reducer<Erc20Event, Erc20Balance> {
    override suspend fun reduce(entity: Erc20Balance, event: Erc20Event): Erc20Balance {

        // For the first snapshot we should determine createdAt date
        val createdAt = if (entity.createdAt == Instant.EPOCH) {
            event.date.toInstant()
        } else {
            entity.createdAt
        }

        val balance = when (event) {
            is Erc20Event.Erc20IncomeTransferEvent -> entity.balance + event.value
            is Erc20Event.Erc20OutcomeTransferEvent -> entity.balance - event.value
            is Erc20Event.Erc20DepositEvent -> entity.balance + event.value
            is Erc20Event.Erc20WithdrawalEvent -> entity.balance - event.value
            is Erc20Event.Erc20TokenApprovalEvent -> entity.balance
        }
        return entity
            .copy(createdAt = createdAt)
            .withBalanceAndLastUpdatedAt(balance, event.date.toInstant())
            .withBlockNumber(blockNumber = event.log.blockNumber)
    }
}
