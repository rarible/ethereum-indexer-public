package com.rarible.protocol.nft.core.service.ownership.reduce.forward

import com.rarible.core.entity.reducer.exception.ReduceException
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import com.rarible.protocol.nft.core.repository.item.ItemRepository
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component

@Component
class ForwardCreatorsOwnershipReducer(
    private val itemRepository: ItemRepository
) : Reducer<OwnershipEvent, Ownership> {

    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        val item = itemRepository
            .findById(ItemId(entity.token, entity.tokenId)).awaitFirstOrNull()
            ?: throw ReduceException("Can't find item for ownership ${entity.id}")

        return if (item.creators != entity.creators) {
            entity.copy(creators = item.creators)
        } else {
            entity
        }
    }
}
