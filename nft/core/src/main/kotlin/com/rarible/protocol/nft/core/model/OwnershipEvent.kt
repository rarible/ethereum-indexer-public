package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

sealed class OwnershipEvent : EthereumEntityEvent<OwnershipEvent>() {
    abstract val value: EthUInt256

    data class TransferToEvent(
        val from: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : OwnershipEvent() {
        fun isMint(): Boolean = from == Address.ZERO()
    }

    data class TransferFromEvent(
        val to: Address,
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : OwnershipEvent() {
        fun isBurn(): Boolean = to == Address.ZERO()
    }

    data class ChangeLazyValueEvent(
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : OwnershipEvent()

    data class LazyTransferToEvent(
        override val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : OwnershipEvent()
}
