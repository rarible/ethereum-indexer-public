package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.converters.model.ItemEventInverter
import scalether.domain.Address

sealed class ItemEvent : EthereumEntityEvent<ItemEvent>() {

    data class ItemMintEvent(
        val supply: EthUInt256,
        val owner: Address,
        override val entityId: String,
        override val log: EthereumLog,
        /**
         * Token URI. Applicable only to pending logs.
         */
        val tokenUri: String? = null
    ) : ItemEvent() {
        override fun invert(): ItemBurnEvent = ItemEventInverter.invert(this)
    }

    data class ItemBurnEvent(
        val owner: Address,
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : ItemEvent() {
        override fun invert(): ItemMintEvent = ItemEventInverter.invert(this)
    }

    data class ItemTransferEvent(
        val from: Address,
        val to: Address,
        val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : ItemEvent() {
        override fun invert(): ItemTransferEvent = ItemEventInverter.invert(this)
    }

    data class ItemCreatorsEvent(
        val creators: List<Part>,
        override val entityId: String,
        override val log: EthereumLog
    ) : ItemEvent() {
        override fun invert(): ItemCreatorsEvent = this
    }

    data class LazyItemMintEvent(
        val supply: EthUInt256,
        val creators: List<Part>,
        override val entityId: String,
        override val log: EthereumLog
    ) : ItemEvent()

    data class LazyItemBurnEvent(
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog
    ) : ItemEvent()

}
