package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import scalether.abi.Uint256Type
import scalether.domain.Address

abstract class AbstractOpenSeaLazyValueItemReducer : Reducer<ItemEvent, Item> {

    protected abstract suspend fun reduceItemTransferEvent(entity: Item, event: ItemEvent.OpenSeaLazyItemMintEvent): Item

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.OpenSeaLazyItemMintEvent -> reduceItemTransferEvent(entity, event)
            is ItemEvent.ItemMintEvent,
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent,
            is ItemEvent.ItemTransferEvent -> entity
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }

    protected fun getTokenCreator(tokenId: EthUInt256): Address {
        val minter = Uint256Type.encode(tokenId.value).slice(0, 20)
        return Address.apply(minter)
    }
}
