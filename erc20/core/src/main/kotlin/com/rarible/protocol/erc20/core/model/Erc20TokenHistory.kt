package com.rarible.protocol.erc20.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address
import java.util.Date

enum class EventType {
    INCOME_TRANSFER,
    OUTCOME_TRANSFER,
    DEPOSIT,
    WITHDRAWAL,
    APPROVAL,
    ;
}

@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "INCOME_TRANSFER", value = Erc20IncomeTransfer::class),
    JsonSubTypes.Type(name = "OUTCOME_TRANSFER", value = Erc20OutcomeTransfer::class),
    JsonSubTypes.Type(name = "DEPOSIT", value = Erc20Deposit::class),
    JsonSubTypes.Type(name = "WITHDRAWAL", value = Erc20Withdrawal::class),
    JsonSubTypes.Type(name = "APPROVAL", value = Erc20Deposit::class)
)
sealed class Erc20TokenHistory(
    var type: EventType
) : EventData {
    abstract val token: Address
    abstract val owner: Address
    abstract val value: EthUInt256
    abstract val date: Date

    override fun getKey(log: EthereumLog): String {
        return BalanceId(token, owner).stringValue
    }
}

data class Erc20IncomeTransfer(
    override val owner: Address,
    override val value: EthUInt256,
    override val token: Address,
    override val date: Date
) : Erc20TokenHistory(
    type = EventType.INCOME_TRANSFER
)

data class Erc20OutcomeTransfer(
    override val owner: Address,
    override val value: EthUInt256,
    override val token: Address,
    override val date: Date
) : Erc20TokenHistory(
    type = EventType.OUTCOME_TRANSFER
)

data class Erc20Deposit(
    override val owner: Address,
    override val value: EthUInt256,
    override val token: Address,
    override val date: Date
) : Erc20TokenHistory(
    type = EventType.DEPOSIT
)

data class Erc20Withdrawal(
    override val owner: Address,
    override val value: EthUInt256,
    override val token: Address,
    override val date: Date
) : Erc20TokenHistory(
    type = EventType.WITHDRAWAL
)

data class Erc20TokenApproval(
    override val owner: Address,
    val spender: Address,
    override val value: EthUInt256,
    override val token: Address,
    override val date: Date
) : Erc20TokenHistory(
    type = EventType.APPROVAL
)
