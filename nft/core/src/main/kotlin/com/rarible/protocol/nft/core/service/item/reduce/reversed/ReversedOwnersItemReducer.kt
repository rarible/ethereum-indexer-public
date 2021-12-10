package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.exception.ReduceException
import com.rarible.core.entity.reducer.service.ReversedReducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ReversedOwnersItemReducer(
    private val ownershipRepository: OwnershipRepository
) : ReversedReducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemTransferEvent-> {
                val owners = entity.owners.toMutableList()

                val to = getOwnership(entity, event.to)
                val from = getOwnership(entity, event.from)

                if (to != null && to.value == event.value) {
                    owners.remove(event.to)
                }
                if (from != null && owners.contains(event.from).not()) {
                    owners.add(event.from)
                }
                entity.copy(owners = owners)
            }
            is ItemEvent.ItemMintEvent,
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.ItemCreatorsEvent -> entity

            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw ReduceException("This events can't be in this reducer")
        }
    }

    private suspend fun getOwnership(entity: Item, owner: Address): Ownership? {
        return ownershipRepository.findById(OwnershipId(entity.token, entity.tokenId, owner)).awaitFirstOrNull()
    }
}
