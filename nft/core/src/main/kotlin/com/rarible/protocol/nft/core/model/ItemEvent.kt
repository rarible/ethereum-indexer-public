package com.rarible.protocol.nft.core.model

import com.rarible.ethereum.domain.EthUInt256

sealed class ItemEvent : BlockchainEntityEvent<ItemEvent>() {
    abstract val supply: EthUInt256

    data class ItemMintEvent(
        override val supply: EthUInt256,
        override val blockNumber: Long,
        override val logIndex: Int,
        override val status: Status,
        override val entityId: String
    ) : ItemEvent()

    data class ItemBurnEvent(
        override val supply: EthUInt256,
        override val blockNumber: Long,
        override val logIndex: Int,
        override val status: Status,
        override val entityId: String
   ) : ItemEvent()
}
