package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import scalether.domain.Address

sealed class ItemEvent : BlockchainEntityEvent<ItemEvent>() {

    data class ItemMintEvent(
        val supply: EthUInt256,
        val owner: Address,
        override val blockNumber: Long,
        override val logIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val minorLogIndex: Int
    ) : ItemEvent()

    data class LazyItemMintEvent(
        val supply: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val minorLogIndex: Int
    ) : ItemEvent()

    data class ItemBurnEvent(
        val supply: EthUInt256,
        override val blockNumber: Long,
        override val logIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val minorLogIndex: Int
   ) : ItemEvent()

    data class LazyItemBurnEvent(
        val supply: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val minorLogIndex: Int
    ) : ItemEvent()

    data class ItemCreatorsEvent(
        val creators: List<Part>,
        override val blockNumber: Long,
        override val logIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val minorLogIndex: Int
    ) : ItemEvent()
}

