package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.protocol.order.core.model.AggregatorFetchState
import com.rarible.protocol.order.core.model.X2Y2CancelListEventFetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2EventLoadProperties
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Event
import java.time.Instant

class X2Y2CancelEventsLoadHandler(
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val x2y2CancelEventLoader: X2Y2CancelListEventLoader,
    private val properties: X2Y2EventLoadProperties
) : AbstractX2Y2LoadHandler<Event>(aggregatorStateRepository, properties) {

    override suspend fun getState(): AggregatorFetchState? {
        return aggregatorStateRepository.getX2Y2CancelListEventState()
    }

    override suspend fun getResult(cursor: String): ApiListResponse<Event> {
        return x2y2CancelEventLoader.load(cursor)
    }

    override fun getDefaultFetchState(): AggregatorFetchState {
        return X2Y2CancelListEventFetchState(
            cursor = codeCursor((properties.startCursor ?: Instant.now().epochSecond))
        )
    }
}
