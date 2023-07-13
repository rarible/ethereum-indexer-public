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

    abstract fun withSupply(supply: EthUInt256): ItemEvent

    abstract fun withCompact(compact: Boolean): ItemEvent

    abstract fun supply(): EthUInt256


    data class ItemMintEvent(
        val supply: EthUInt256,
        val owner: Address,
        override val entityId: String,
        override val log: EthereumLog,
        /**
         * Token URI. Applicable only to pending logs.
         */
        val tokenUri: String? = null,
        override val compact: Boolean = false,
    ) : ItemEvent() {

        override fun invert(): ItemBurnEvent = ItemEventInverter.invert(this)

        override fun withSupply(supply: EthUInt256) = copy(supply = supply)

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = supply
    }

    data class ItemBurnEvent(
        val owner: Address,
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : ItemEvent() {
        override fun invert(): ItemMintEvent = ItemEventInverter.invert(this)

        override fun withSupply(supply: EthUInt256) = copy(supply = supply)

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = supply
    }

    data class ItemTransferEvent(
        val from: Address,
        val to: Address,
        val value: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : ItemEvent() {

        override fun invert(): ItemTransferEvent = ItemEventInverter.invert(this)

        override fun withSupply(supply: EthUInt256) = copy(value = supply)

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = value
    }

    data class ItemCreatorsEvent(
        val creators: List<Part>,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : ItemEvent() {
        override fun invert(): ItemCreatorsEvent = this

        override fun withSupply(supply: EthUInt256) = this

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = EthUInt256.ZERO
    }

    data class OpenSeaLazyItemMintEvent(
        val from: Address,
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : ItemEvent() {

        override fun withSupply(supply: EthUInt256) = this.copy(supply = supply)

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = supply
    }

    data class LazyItemMintEvent(
        val supply: EthUInt256,
        val creators: List<Part>,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : ItemEvent() {

        override fun withSupply(supply: EthUInt256) = this.copy(supply = supply)

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = supply
    }

    data class LazyItemBurnEvent(
        val supply: EthUInt256,
        override val entityId: String,
        override val log: EthereumLog,
        override val compact: Boolean = false,
    ) : ItemEvent() {

        override fun withSupply(supply: EthUInt256) = this.copy(supply = supply)

        override fun withCompact(compact: Boolean) = copy(compact = compact)

        override fun supply() = supply
    }
}
