package com.rarible.protocol.nft.core.model

import com.rarible.blockchain.scanner.ethereum.model.EthereumEntityEvent
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.core.common.EventTimeMarks
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.converters.model.ItemEventInverter
import org.springframework.data.annotation.Transient
import scalether.domain.Address

sealed class ItemEvent : EthereumEntityEvent<ItemEvent>() {

    @Volatile
    @Transient
    var eventTimeMarks: EventTimeMarks? = null

    sealed class ItemSupplyEvent : ItemEvent() {
        abstract val supply: EthUInt256

        abstract fun withSupply(supply: EthUInt256): ItemSupplyEvent
    }

    data class ItemMintEvent(
        override val supply: EthUInt256,
        val owner: Address,
        override val entityId: String,
        override val log: EthereumLog,
        /**
         * Token URI. Applicable only to pending logs.
         */
        val tokenUri: String? = null
    ) : ItemSupplyEvent() {
        override fun invert(): ItemBurnEvent = ItemEventInverter.invert(this)

        override fun withSupply(supply: EthUInt256) = copy(supply = supply)
    }

    data class ItemBurnEvent(
        val owner: Address,
        override val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : ItemSupplyEvent() {
        override fun invert(): ItemMintEvent = ItemEventInverter.invert(this)

        override fun withSupply(supply: EthUInt256) = copy(supply = supply)
    }

    data class ItemTransferEvent(
        val from: Address,
        val to: Address,
        val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : ItemEvent() {
        override fun invert(): ItemTransferEvent = ItemEventInverter.invert(this)
    }

    data class ItemCreatorsEvent(
        val creators: List<Part>,
        override val entityId: String,
        override val log: EthereumLog,
    ) : ItemEvent() {
        override fun invert(): ItemCreatorsEvent = this
    }

    data class OpenSeaLazyItemMintEvent(
        val from: Address,
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : ItemEvent()

    data class LazyItemMintEvent(
        val supply: EthUInt256,
        val creators: List<Part>,
        override val entityId: String,
        override val log: EthereumLog,
    ) : ItemEvent()

    data class LazyItemBurnEvent(
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
    ) : ItemEvent()
}
