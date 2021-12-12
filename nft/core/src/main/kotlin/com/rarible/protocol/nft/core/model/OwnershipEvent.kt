package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256

sealed class OwnershipEvent : BlockchainEntityEvent<OwnershipEvent>() {
    abstract val value: EthUInt256

    data class TransferToEvent(
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

    data class TransferFromEvent(
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
