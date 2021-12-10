package com.rarible.protocol.nft.core.service.item.reduce.forward

import com.rarible.core.entity.reducer.exception.ReduceException
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipId
import com.rarible.protocol.nft.core.repository.ownership.OwnershipRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
class ForwardOwnersItemReducer(
    private val ownershipRepository: OwnershipRepository
) : Reducer<ItemEvent, Item> {

    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemTransferEvent-> {
                val owners = entity.owners.toMutableList()

                val to = getOwnership(entity, event.to)
                val from = getOwnership(entity, event.from)

                if (to == null || owners.contains(event.to).not()) {
                    owners.add(event.to)
                }
                if (from != null && from.value == event.value) {
                    owners.remove(event.from)
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
