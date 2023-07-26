package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.EventTimeMarks
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20Event
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20MarkedEvent
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import org.springframework.stereotype.Component

@Component
class Erc20EventConverter {

    fun convert(source: ReversedEthereumLogRecord, eventTimeMarks: EventTimeMarks? = null): Erc20MarkedEvent? {
        val event = when (val data = source.data as? Erc20TokenHistory) {
            is Erc20IncomeTransfer -> Erc20Event.Erc20IncomeTransferEvent(
                entityId = data.getKey(source.log),
                log = source.log,
                owner = data.owner,
                value = data.value,
                token = data.token,
                date = data.date
            )

            is Erc20OutcomeTransfer -> Erc20Event.Erc20OutcomeTransferEvent(
                entityId = data.getKey(source.log),
                log = source.log,
                owner = data.owner,
                value = data.value,
                token = data.token,
                date = data.date
            )

            is Erc20Deposit -> Erc20Event.Erc20DepositEvent(
                entityId = data.getKey(source.log),
                log = source.log,
                owner = data.owner,
                value = data.value,
                token = data.token,
                date = data.date
            )

            is Erc20Withdrawal -> Erc20Event.Erc20WithdrawalEvent(
                entityId = data.getKey(source.log),
                log = source.log,
                owner = data.owner,
                value = data.value,
                token = data.token,
                date = data.date
            )

            is Erc20TokenApproval -> Erc20Event.Erc20TokenApprovalEvent(
                entityId = data.getKey(source.log),
                log = source.log,
                owner = data.owner,
                value = data.value,
                token = data.token,
                date = data.date,
                spender = data.spender
            )

            null -> null
        }
        return event?.let { Erc20MarkedEvent(event, eventTimeMarks) }
    }
}
