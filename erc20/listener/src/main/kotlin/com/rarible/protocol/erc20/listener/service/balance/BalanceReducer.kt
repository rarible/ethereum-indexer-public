package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.erc20.core.model.*
import org.springframework.stereotype.Component

@Component
class BalanceReducer : Reducer<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId> {

    override suspend fun reduce(initial: BalanceReduceSnapshot, event: Erc20ReduceEvent): BalanceReduceSnapshot {
        val logEvent = event.logEvent
        val initialBalance = initial.data.balance

        val balance = if (logEvent.status == LogEventStatus.CONFIRMED) {
            when (val data = logEvent.data) {
                is Erc20IncomeTransfer -> initialBalance + data.value
                is Erc20OutcomeTransfer -> initialBalance - data.value
                is Erc20Deposit -> initialBalance +  data.value
                is Erc20Withdrawal -> initialBalance - data.value
                is Erc20TokenApproval -> initialBalance
                else -> throw IllegalArgumentException("Unexpected data type ${data.javaClass}")
            }
        } else {
            initialBalance
        }

        return BalanceReduceSnapshot(
            id = initial.id,
            data = initial.data.withBalance(balance),
            mark = event.mark
        )
    }

    override fun getDataKeyFromEvent(event: Erc20ReduceEvent): BalanceId {
        return when (val data = event.logEvent.data) {
            is Erc20TokenHistory -> BalanceId(token = data.token, owner = data.owner)
            else -> throw IllegalArgumentException("Unexpected history data type ${data.javaClass}")
        }
    }

    override fun getInitialData(key: BalanceId): BalanceReduceSnapshot {
        return BalanceReduceSnapshot(
            id = key,
            data = Erc20Balance(
                token = key.token,
                owner = key.owner,
                balance = EthUInt256.ZERO
            ),
            mark = Long.MIN_VALUE
        )
    }
}

