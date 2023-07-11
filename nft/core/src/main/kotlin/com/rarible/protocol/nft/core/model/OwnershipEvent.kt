package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.core.common.EventTimeMarks
import com.rarible.ethereum.domain.EthUInt256
import org.springframework.data.annotation.Transient
import scalether.domain.Address

sealed class OwnershipEvent : EthereumEntityEvent<OwnershipEvent>() {
    abstract val value: EthUInt256

    abstract fun withValue(value: EthUInt256): OwnershipEvent

    @Transient
    @Volatile
    var eventTimeMarks: EventTimeMarks? = null

    data class TransferToEvent(
        val from: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : OwnershipEvent() {
        fun isMint(): Boolean = from == Address.ZERO()

        override fun withValue(value: EthUInt256) = copy(value = value)
    }

    data class TransferFromEvent(
        val to: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : OwnershipEvent() {
        fun isBurn(): Boolean = to == Address.ZERO()

        override fun withValue(value: EthUInt256) = copy(value = value)
    }

    data class ChangeLazyValueEvent(
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : OwnershipEvent() {

        override fun withValue(value: EthUInt256) = copy(value = value)
    }

    data class LazyTransferToEvent(
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : OwnershipEvent() {

        override fun withValue(value: EthUInt256) = copy(value = value)
    }

    data class LazyBurnEvent(
        val from: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : OwnershipEvent() {

        override fun withValue(value: EthUInt256) = copy(value = value)
    }
}
