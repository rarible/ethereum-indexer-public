package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.LooksrareV2FetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class LooksrareOrderLoadHandler(
    private val looksrareOrderLoader: LooksrareOrderLoader,
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val properties: LooksrareLoadProperties
) : JobHandler {

    override suspend fun handle() {
        val state = aggregatorStateRepository.getLooksrareV2State() ?: getDefaultFetchState()
        val cursor = state.looksrareV2Cursor
        val result = looksrareOrderLoader.load(cursor)
        if (result.cursor != null) {
            aggregatorStateRepository.save(state.withCursor(result.cursor))
        }
        if (result.saved == 0L && result.cursor?.nextId == null) {
            delay(properties.pollingPeriod)
        }
    }

    private fun getDefaultFetchState(): LooksrareV2FetchState {
        return LooksrareV2FetchState(
            cursorObj = LooksrareV2Cursor(Instant.now() - Duration.ofHours(1))
        )
    }
}
