package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.converters.model.ItemEventInverter
import scalether.domain.Address

sealed class ItemEvent : BlockchainEntityEvent<ItemEvent>() {

    data class ItemMintEvent(
        val supply: EthUInt256,
        val owner: Address,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : ItemEvent() {
        override fun invert(): ItemBurnEvent = ItemEventInverter.invert(this)
    }

    data class ItemBurnEvent(
        val owner: Address,
        val supply: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : ItemEvent() {
        override fun invert(): ItemMintEvent = ItemEventInverter.invert(this)
    }

    data class ItemTransferEvent(
        val from: Address,
        val to: Address,
        val value: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : ItemEvent() {
        override fun invert(): ItemTransferEvent = ItemEventInverter.invert(this)
    }

    data class ItemCreatorsEvent(
        val creators: List<Part>,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : ItemEvent() {
        override fun invert(): ItemCreatorsEvent = this
    }

    data class LazyItemMintEvent(
        val supply: EthUInt256,
        val creators: List<Part>,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : ItemEvent()

    data class LazyItemBurnEvent(
        val supply: EthUInt256,
        override val blockNumber: Long?,
        override val logIndex: Int?,
        override val minorLogIndex: Int,
        override val status: Status,
        override val entityId: String,
        override val timestamp: Long,
        override val transactionHash: String,
        override val address: String
    ) : ItemEvent()

}

