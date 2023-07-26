package com.rarible.protocol.nft.core.repository.item

import com.rarible.protocol.nft.core.model.ItemExState
import com.rarible.protocol.nft.core.model.ItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
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

    fun getAll(from: ItemId?): Flow<ItemExState> {
        val criteria = from?.let { Criteria.where(ID_FILED).gt(it) } ?: Criteria()
        val query = Query(criteria).with(SORT_ID_ASC)
        return template.find(query, ItemExState::class.java).asFlow()
    }

    private companion object {
        const val ID_FILED = "_id"
        val SORT_ID_ASC: Sort = Sort.by(Sort.Order.asc(ID_FILED))
    }
}
