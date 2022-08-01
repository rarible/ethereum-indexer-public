package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.daemon.job.JobHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.order.core.model.LooksrareFetchState
import com.rarible.protocol.order.core.repository.looksrare.LooksrareFetchStateRepository
import com.rarible.protocol.order.listener.configuration.LooksrareLoadProperties
import kotlinx.coroutines.time.delay
import java.time.Instant

class LooksrareOrderLoadHandler(
    private val looksrareOrderLoader: LooksrareOrderLoader,
    private val looksrareFetchStateRepository: LooksrareFetchStateRepository,
    private val properties: LooksrareLoadProperties,
) : JobHandler {

    override suspend fun handle() {
        val state = looksrareFetchStateRepository.get(STATE_ID_PREFIX) ?: getDefaultFetchState()
        val cursor = state.cursor
        val result = looksrareOrderLoader.load(cursor)

        if (result.data.isEmpty()) {
            delay(properties.pollingPeriod)
        } else {
            looksrareFetchStateRepository.save(state.withCursor(result.data.last().hash.hex()))
        }
    }

    private fun getDefaultFetchState(): LooksrareFetchState {
        return LooksrareFetchState(id = STATE_ID_PREFIX, cursor = null, listedAfter = Instant.EPOCH)
    }

    private companion object {
        val logger by Logger()
        const val STATE_ID_PREFIX = "looksrare_order_fetch"
    }
}

