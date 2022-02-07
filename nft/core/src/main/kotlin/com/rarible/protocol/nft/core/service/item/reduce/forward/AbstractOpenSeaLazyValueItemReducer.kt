package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger

abstract class AbstractOpenSeaLazyValueItemReducer(
    private val openSeaLazyMintAddress: Address
) : Reducer<ItemEvent, Item> {

    protected abstract suspend  fun reduceItemTransferEvent(entity: Item, event: ItemEvent.ItemTransferEvent): Item

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return if (event.log.address == openSeaLazyMintAddress && isLazyMintTokenAddress(entity.tokenId)) {
            when (event) {
                is ItemEvent.ItemTransferEvent -> reduceItemTransferEvent(entity, event)
                is ItemEvent.ItemMintEvent,
                is ItemEvent.ItemBurnEvent,
                is ItemEvent.ItemCreatorsEvent -> entity
                is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                    throw IllegalArgumentException("This events can't be in this reducer")
            }
        } else {
            entity
        }
    }

    private fun isLazyMintTokenAddress(tokenId: EthUInt256): Boolean {
        return (tokenId.value < BigInteger.valueOf(2).pow(96)).not()
    }

    protected fun getTokenCreator(tokenId: EthUInt256): Address {
        val minter = Uint256Type.encode(tokenId.value).slice(0, 20)
        return Address.apply(minter)
    }
}