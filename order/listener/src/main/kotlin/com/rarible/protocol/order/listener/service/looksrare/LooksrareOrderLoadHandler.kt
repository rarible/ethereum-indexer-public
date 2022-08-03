package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.LooksrareFetchState
import com.rarible.protocol.order.core.repository.looksrare.LooksrareFetchStateRepository
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import kotlinx.coroutines.time.delay
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class LooksrareOrderLoadHandler(
    private val looksrareOrderLoader: LooksrareOrderLoader,
    private val looksrareFetchStateRepository: LooksrareFetchStateRepository,
    private val properties: LooksrareLoadProperties
) : JobHandler {

    override suspend fun handle() {
        val state = looksrareFetchStateRepository.get(STATE_ID_PREFIX) ?: getDefaultFetchState()
        val listedAfter = state.listedAfter
        val listedBefore = minOf(state.listedAfter + properties.loadPeriod, Instant.now())
        val result = looksrareOrderLoader.load(listedAfter = listedAfter, listedBefore = listedBefore)
        if (result.isEmpty()) {
            delay(properties.pollingPeriod)
        }
        looksrareFetchStateRepository.save(state.withListedAfter(listedBefore))
    }

    private fun getDefaultFetchState(): LooksrareFetchState {
        return LooksrareFetchState(
            id = STATE_ID_PREFIX,
            listedAfter = Instant.now() - properties.delay - properties.loadPeriod
        )
    }

    internal companion object {
        const val STATE_ID_PREFIX = "looksrare_order_fetch"
    }
}

