package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.ItemExState
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@Component
class ItemExStateRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun getById(id: ItemId): ItemExState? {
        return template.findById(id, ItemExState::class.java).awaitFirstOrNull()
    }

    suspend fun save(state: ItemExState): ItemExState {
        return template.save(state).awaitFirst()
    }
}