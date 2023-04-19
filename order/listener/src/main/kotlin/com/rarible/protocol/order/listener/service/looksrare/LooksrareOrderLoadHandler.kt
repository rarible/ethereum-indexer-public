package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.LooksrareV2FetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
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
        val createdAfter = state.createdAfter
        val result = looksrareOrderLoader.load(createdAfter = createdAfter)
        if (result.latestCreatedAt != null) {
            aggregatorStateRepository.save(state.withCreatedAfter(result.latestCreatedAt))
        }
        if (result.saved == 0L) {
            delay(properties.pollingPeriod)
        }
    }

    private fun getDefaultFetchState(): LooksrareV2FetchState {
        return LooksrareV2FetchState.withCreatedAfter(
            createdAfter = Instant.now() - Duration.ofHours(1)
        )
    }
}

