package com.rarible.protocol.erc20.core.service.reduce

import com.rarible.blockchain.scanner.ethereum.reduce.AbstractMetricReducer
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Event
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class Erc20BalanceMetricReducer(
    properties: Erc20IndexerProperties,
    meterRegistry: MeterRegistry,
) : AbstractMetricReducer<Erc20Event, Erc20Balance>(properties, meterRegistry, "erc20") {

    override fun getMetricName(event: Erc20Event): String {
        return when (event) {
            is Erc20Event.Erc20IncomeTransferEvent -> "income"
            is Erc20Event.Erc20OutcomeTransferEvent -> "outcome"
            is Erc20Event.Erc20DepositEvent -> "deposit"
            is Erc20Event.Erc20WithdrawalEvent -> "withdrawal"
            is Erc20Event.Erc20TokenApprovalEvent -> "approval"
        }
    }
}
