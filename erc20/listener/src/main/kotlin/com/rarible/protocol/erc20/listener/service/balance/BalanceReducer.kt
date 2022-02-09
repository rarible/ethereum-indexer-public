package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.core.reduce.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.BalanceReduceSnapshot
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20ReduceEvent
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class BalanceReducer : Reducer<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId> {

    override suspend fun reduce(current: BalanceReduceSnapshot, event: Erc20ReduceEvent): BalanceReduceSnapshot {
        val logEvent = event.logEvent
        val currentData = current.data
        val currentBalance = currentData.balance

        if (logEvent.status != LogEventStatus.CONFIRMED) {
            return current.copy(mark = event.mark)
        }

        val data = logEvent.data

        if (!(data is Erc20TokenHistory)) {
            throw IllegalArgumentException("Unexpected data type ${data.javaClass}")
        }

        val eventDate = data.date.toInstant()

        val balance = when (data) {
            is Erc20IncomeTransfer -> currentBalance + data.value
            is Erc20OutcomeTransfer -> currentBalance - data.value
            is Erc20Deposit -> currentBalance + data.value
            is Erc20Withdrawal -> currentBalance - data.value
            is Erc20TokenApproval -> currentBalance
        }

        // For the first snapshot we should determine createdAt date
        val updatedData = if (current.data.createdAt == Instant.EPOCH) {
            current.data.copy(createdAt = eventDate)
        } else {
            current.data
        }.withBalanceAndLastUpdatedAt(balance, eventDate)

        return BalanceReduceSnapshot(
            id = current.id,
            data = updatedData,
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
                balance = EthUInt256.ZERO,
                createdAt = Instant.EPOCH,
                lastUpdatedAt = Instant.EPOCH
            ),
            mark = Long.MIN_VALUE
        )
    }
}

