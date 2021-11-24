package com.rarible.protocol.order.core.repository.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@CaptureSpan(type = SpanType.DB)
@Component
class OpenSeaFetchStateRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(fetchState: OpenSeaFetchState) {
        template.save(fetchState).awaitFirst()
    }

    suspend fun get(): OpenSeaFetchState? {
        return template.findById(OpenSeaFetchState.ID, OpenSeaFetchState::class.java).awaitFirstOrNull()
    }

    suspend fun delete() {
        val query = Query(Criteria.where("_id").isEqualTo(OpenSeaFetchState.ID))
        template.remove(query, OpenSeaFetchState::class.java).awaitFirst()
    }
}
