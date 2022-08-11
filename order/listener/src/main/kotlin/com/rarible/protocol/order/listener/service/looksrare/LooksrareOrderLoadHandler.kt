package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.LooksrareFetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderLoadHandler(
    private val looksrareOrderLoader: LooksrareOrderLoader,
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val properties: LooksrareLoadProperties
) : JobHandler {

    override suspend fun handle() {
        val state = aggregatorStateRepository.getLooksrareState() ?: getDefaultFetchState()
        val listedAfter = state.listedAfter
        val listedBefore = minOf(state.listedAfter + properties.loadPeriod, Instant.now() - properties.delay)
        val result = looksrareOrderLoader.load(listedAfter = listedAfter, listedBefore = listedBefore)
        if (result.isEmpty()) {
            delay(properties.pollingPeriod)
        }
        aggregatorStateRepository.save(state.withListedAfter(listedBefore))
    }

    private fun getDefaultFetchState(): LooksrareFetchState {
        return LooksrareFetchState.withListedAfter(
            listedAfter = Instant.now() - properties.delay - properties.loadPeriod
        )
    }
}

