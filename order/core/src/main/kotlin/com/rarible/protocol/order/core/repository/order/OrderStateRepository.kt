package com.rarible.protocol.order.core.repository.order

import com.rarible.protocol.order.core.model.OrderState
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
class OrderStateRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun getById(id: Word): OrderState? {
        return template.findById(id, OrderState::class.java).awaitFirstOrNull()
    }

    suspend fun save(state: OrderState): OrderState {
        return template.save(state).awaitFirst()
    }

    suspend fun remove(hash: Word): Boolean {
        val criteria = Criteria.where("_id").isEqualTo(hash)
        return template.remove(Query(criteria), OrderState::class.java).awaitFirst().deletedCount > 0
    }
}
