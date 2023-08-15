package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.model.LooksrareV2CancelListEventFetchState
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.LooksrareV2State
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class LooksrareCancelListEventLoadHandler(
    private val looksrareCancelListEventLoader: LooksrareCancelListEventLoader,
    private val aggregatorStateRepository: AggregatorStateRepository,
    properties: LooksrareLoadProperties,
) : LooksrareLoadHandler(aggregatorStateRepository = aggregatorStateRepository, properties = properties) {
    override suspend fun getState(): LooksrareV2State =
        aggregatorStateRepository.getLooksrareV2CancelListEventState() ?: getDefaultFetchState()

    override suspend fun load(cursor: LooksrareV2Cursor): Result = looksrareCancelListEventLoader.load(cursor)

    private fun getDefaultFetchState(): LooksrareV2CancelListEventFetchState {
        return LooksrareV2CancelListEventFetchState(
            cursorObj = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))
        )
    }
}
