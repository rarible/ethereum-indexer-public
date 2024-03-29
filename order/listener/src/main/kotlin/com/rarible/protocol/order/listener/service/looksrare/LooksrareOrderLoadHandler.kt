package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.LooksrareV2FetchState
import com.rarible.protocol.order.core.model.LooksrareV2State
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class LooksrareOrderLoadHandler(
    private val looksrareOrderLoader: LooksrareOrderLoader,
    private val aggregatorStateRepository: AggregatorStateRepository,
    properties: LooksrareLoadProperties
) : LooksrareLoadHandler(aggregatorStateRepository = aggregatorStateRepository, properties = properties) {
    override suspend fun getState(): LooksrareV2State =
        aggregatorStateRepository.getLooksrareV2State() ?: getDefaultFetchState()

    override suspend fun load(cursor: LooksrareV2Cursor): Result = looksrareOrderLoader.load(cursor)

    private fun getDefaultFetchState(): LooksrareV2FetchState {
        return LooksrareV2FetchState(
            cursorObj = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))
        )
    }
}
