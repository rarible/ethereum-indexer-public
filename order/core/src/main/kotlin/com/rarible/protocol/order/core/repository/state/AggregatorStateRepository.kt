package com.rarible.protocol.order.core.repository.state

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.AggregatorFetchState
import com.rarible.protocol.order.core.model.LooksrareFetchState
import com.rarible.protocol.order.core.model.SeaportFetchState
import com.rarible.protocol.order.core.model.X2Y2FetchState
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@CaptureSpan(type = SpanType.DB)
@Component
class AggregatorStateRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun <T: AggregatorFetchState> save(fetchState: T) {
        template.save(fetchState, COLLECTION).awaitFirst()
    }

    suspend fun getSeaportState(): SeaportFetchState? {
        return get(SeaportFetchState.ID, SeaportFetchState::class.java)
    }

    suspend fun getLooksrareState(): LooksrareFetchState? {
        return get(LooksrareFetchState.ID, LooksrareFetchState::class.java)
    }

    suspend fun getX2Y2State(): X2Y2FetchState? {
        return get(X2Y2FetchState.ID, X2Y2FetchState::class.java)
    }

    private suspend fun <T: AggregatorFetchState> get(id: String, type: Class<T>): T? {
        return template.findById(id, type, COLLECTION).awaitFirstOrNull()
    }

    private companion object {
        const val COLLECTION = "order_fetch_state"
    }
}