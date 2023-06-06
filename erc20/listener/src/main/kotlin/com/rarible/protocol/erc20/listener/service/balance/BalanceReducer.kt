package com.rarible.protocol.erc20.listener.service.balance

import com.rarible.blockchain.scanner.ethereum.model.EthereumLogStatus
import com.rarible.core.reduce.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
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
import com.rarible.protocol.erc20.listener.configuration.Erc20ListenerProperties
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.time.Instant

@Component
class BalanceReducer(
    props: Erc20ListenerProperties
) : Reducer<Erc20ReduceEvent, BalanceReduceSnapshot, Long, Erc20Balance, BalanceId> {

    val depositContracts = props.depositTokens.map { Address.apply(it) }.toSet()

    override suspend fun reduce(current: BalanceReduceSnapshot, event: Erc20ReduceEvent): BalanceReduceSnapshot {
        val logEvent = event.logEvent
        val currentData = current.data
        val currentBalance = currentData.balance

        if (logEvent.status != EthereumLogStatus.CONFIRMED) {
            return current.copy(mark = event.mark)
        }

        val data = logEvent.data

        if (data !is Erc20TokenHistory) {
            throw IllegalArgumentException("Unexpected data type ${data.javaClass}")
        }

        val eventDate = data.date.toInstant()
        val isDepositContract = depositContracts.contains(data.token)

        val balance = when (data) {
            is Erc20IncomeTransfer -> currentBalance + data.value
            is Erc20OutcomeTransfer -> currentBalance - data.value
            is Erc20Deposit -> when {
                isDepositContract -> currentBalance + data.value
                else -> currentBalance
            }
            is Erc20Withdrawal -> when {
                isDepositContract -> currentBalance - data.value
                else -> currentBalance
            }
            is Erc20TokenApproval -> currentBalance
        }

        // For the first snapshot we should determine createdAt date
        val updatedData = if (current.data.createdAt == Instant.EPOCH) {
            current.data.copy(createdAt = eventDate)
        } else {
            current.data
        }.withBalanceAndLastUpdatedAt(balance, eventDate).withBlockNumber(logEvent.blockNumber)

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
                lastUpdatedAt = Instant.EPOCH,
                revertableEvents = emptyList(), //todo
                blockNumber = null
            ),
            mark = Long.MIN_VALUE
        )
    }
}

