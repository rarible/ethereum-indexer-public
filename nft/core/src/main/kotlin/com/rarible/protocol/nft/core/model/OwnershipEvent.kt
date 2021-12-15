package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

sealed class OwnershipEvent : BlockchainEntityEvent<OwnershipEvent>() {
    abstract val value: EthUInt256

    data class TransferToEvent(
        val from: Address,
        override val value: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : OwnershipEvent() {
        fun isMint(): Boolean = from == Address.ZERO()
    }

    data class TransferFromEvent(
        val to: Address,
        override val value: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : OwnershipEvent() {
        fun isBurn(): Boolean = to == Address.ZERO()
    }

    data class ChangeLazyValueEvent(
        override val value: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : OwnershipEvent()

    data class LazyTransferToEvent(
        override val value: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : OwnershipEvent()
}
