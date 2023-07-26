package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.protocol.order.core.model.AggregatorFetchState
import com.rarible.protocol.order.core.model.X2Y2CancelListEventFetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2LoadProperties
import com.rarible.x2y2.client.model.ApiListResponse
import com.rarible.x2y2.client.model.Order
import java.time.Instant

class X2Y2OrderLoadHandler(
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val x2y2OrderLoader: X2Y2OrderLoader,
    private val properties: X2Y2LoadProperties
) : AbstractX2Y2LoadHandler<Order>(aggregatorStateRepository, properties) {

    override suspend fun getState(): AggregatorFetchState? {
        return aggregatorStateRepository.getX2Y2State()
    }

    override suspend fun getResult(cursor: String): ApiListResponse<Order> {
        return x2y2OrderLoader.load(cursor)
    }

    override fun getDefaultFetchState(): AggregatorFetchState {
        return X2Y2CancelListEventFetchState(
            cursor = codeCursor((properties.startCursor ?: Instant.now().toEpochMilli()))
        )
    }
}
