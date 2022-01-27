package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.service.item.reduce.ReduceContext
import org.springframework.stereotype.Component
import kotlin.coroutines.coroutineContext

@Component
class ForwardOwnersItemReducer : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        val ctx = coroutineContext[ReduceContext]
        return if (ctx != null && ctx.skipOwnerships) {
            entity
        } else {
            val ownerships = entity.ownerships.toMutableMap()

            when (event) {
                is ItemEvent.ItemTransferEvent -> {
                    val fromValue = ownerships[event.from] ?: EthUInt256.ZERO
                    if (fromValue > event.value) {
                        ownerships[event.from] = fromValue - event.value
                    } else {
                        ownerships.remove(event.from)
                    }

                    val toValue = ownerships[event.to] ?: EthUInt256.ZERO
                    if (event.value != EthUInt256.ZERO) {
                        ownerships[event.to] = toValue + event.value
                    }
                }
                is ItemEvent.ItemMintEvent -> {
                    val ownerValue = ownerships[event.owner] ?: EthUInt256.ZERO

                    if (event.supply != EthUInt256.ZERO) {
                        ownerships[event.owner] = ownerValue + event.supply
                    }
                }
                is ItemEvent.ItemBurnEvent -> {
                    val ownerValue = ownerships[event.owner] ?: EthUInt256.ZERO

                    if (ownerValue > event.supply) {
                        ownerships[event.owner] = ownerValue - event.supply
                    } else {
                        ownerships.remove(event.owner)
                    }
                }
                is ItemEvent.ItemCreatorsEvent -> {
                }
                is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                    throw IllegalArgumentException("This events can't be in this reducer")
            }
            entity.copy(ownerships = ownerships)
        }
    }
}
