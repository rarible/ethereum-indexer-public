package com.rarible.protocol.order.core.repository.x2y2

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.order.core.model.X2Y2FetchState
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class X2Y2FetchStateRepository(
    private val mongo: ReactiveMongoTemplate
) {
    suspend fun save(state: X2Y2FetchState): X2Y2FetchState = mongo.save(state).awaitFirst()

    suspend fun get(id: String): X2Y2FetchState? = mongo.findById(id, X2Y2FetchState::class.java).awaitFirstOrNull()
}
