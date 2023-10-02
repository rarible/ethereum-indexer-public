package com.rarible.protocol.order.core.repository.opensea

import com.rarible.protocol.order.core.model.OpenSeaFetchState
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component

@Component
@Deprecated("Remove in release 1.33")
class OpenSeaFetchStateRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun save(fetchState: OpenSeaFetchState) {
        template.save(fetchState).awaitFirst()
    }

    suspend fun get(id: String): OpenSeaFetchState? {
        return template.findById(id, OpenSeaFetchState::class.java).awaitFirstOrNull()
    }

    suspend fun delete() {
        val query = Query(Criteria.where("_id").isEqualTo(OpenSeaFetchState.ID))
        template.remove(query, OpenSeaFetchState::class.java).awaitFirst()
    }
}
