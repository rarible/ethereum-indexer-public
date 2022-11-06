package com.rarible.protocol.erc20.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.service.reduce.Erc20EventInverter
import scalether.domain.Address
import java.util.Date

sealed class Erc20Event : EthereumEntityEvent<Erc20Event>() {

    abstract val token: Address
    abstract val owner: Address
    abstract val value: EthUInt256
    abstract val date: Date

    data class Erc20IncomeTransferEvent(
        override val entityId: String,
        override val log: EthereumLog,

        override val owner: Address,
        override val value: EthUInt256,
        override val token: Address,
        override val date: Date
    ) : Erc20Event() {
        override fun invert(): Erc20Event = Erc20EventInverter.invert(this)
    }

    data class Erc20OutcomeTransferEvent(
        override val entityId: String,
        override val log: EthereumLog,

        override val owner: Address,
        override val value: EthUInt256,
        override val token: Address,
        override val date: Date
    ) : Erc20Event() {
        override fun invert(): Erc20Event = Erc20EventInverter.invert(this)
    }

    data class Erc20DepositEvent(
        override val entityId: String,
        override val log: EthereumLog,

        override val owner: Address,
        override val value: EthUInt256,
        override val token: Address,
        override val date: Date
    ) : Erc20Event() {
        override fun invert(): Erc20Event = Erc20EventInverter.invert(this)
    }

    data class Erc20WithdrawalEvent(
        override val entityId: String,
        override val log: EthereumLog,

        override val owner: Address,
        override val value: EthUInt256,
        override val token: Address,
        override val date: Date
    ) : Erc20Event() {
        override fun invert(): Erc20Event = Erc20EventInverter.invert(this)
    }

    data class Erc20TokenApprovalEvent(
        override val entityId: String,
        override val log: EthereumLog,

        override val owner: Address,
        override val value: EthUInt256,
        override val token: Address,
        override val date: Date,

        val spender: Address
    ) : Erc20Event() {
        override fun invert(): Erc20Event = this
    }
}