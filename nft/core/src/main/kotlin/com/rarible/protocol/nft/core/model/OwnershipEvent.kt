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

    abstract fun withCompact(compact: Boolean): OwnershipEvent

    @Transient
    @Volatile
    var eventTimeMarks: EventTimeMarks? = null

    data class TransferToEvent(
        val from: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : OwnershipEvent() {
        fun isMint(): Boolean = from == Address.ZERO()

        override fun withValue(value: EthUInt256) = copy(value = value)

        override fun withCompact(compact: Boolean) = copy(compact = compact)
    }

    data class TransferFromEvent(
        val to: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : OwnershipEvent() {
        fun isBurn(): Boolean = to == Address.ZERO()

        override fun withValue(value: EthUInt256) = copy(value = value)

        override fun withCompact(compact: Boolean) = copy(compact = compact)
    }

    data class ChangeLazyValueEvent(
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : OwnershipEvent() {

        override fun withValue(value: EthUInt256) = copy(value = value)

        override fun withCompact(compact: Boolean) = copy(compact = compact)
    }

    data class LazyTransferToEvent(
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : OwnershipEvent() {

        override fun withValue(value: EthUInt256) = copy(value = value)

        override fun withCompact(compact: Boolean) = copy(compact = compact)
    }

    data class LazyBurnEvent(
        val from: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : OwnershipEvent() {

        override fun withValue(value: EthUInt256) = copy(value = value)

        override fun withCompact(compact: Boolean) = copy(compact = compact)
    }
}
