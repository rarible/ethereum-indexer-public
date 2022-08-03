package com.rarible.protocol.order.core.repository.looksrare

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.LooksrareFetchState
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@CaptureSpan(type = SpanType.DB)
@Component
class LooksrareFetchStateRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(fetchState: LooksrareFetchState) {
        template.save(fetchState).awaitFirst()
    }

    suspend fun get(id: String): LooksrareFetchState? {
        return template.findById(id, LooksrareFetchState::class.java).awaitFirstOrNull()
    }

    suspend fun delete() {
        val query = Query(Criteria.where("_id").isEqualTo(LooksrareFetchState.ID))
        template.remove(query, LooksrareFetchState::class.java).awaitFirst()
    }
}
