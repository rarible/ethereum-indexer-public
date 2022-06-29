package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.OpenSeaFetchState
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

class SeaportOrderLoadHandler(
    private val seaportOrderLoader: SeaportOrderLoader,
    private val openSeaFetchStateRepository: OpenSeaFetchStateRepository,
    private val properties: SeaportLoadProperties,
) : JobHandler {
    private val stateId = STATE_ID_PREFIX

    override suspend fun handle() {
        val state = openSeaFetchStateRepository.get(stateId) ?: getDefaultFetchState()
        val cursor = state.cursor
        val result = seaportOrderLoader.load(cursor)

        val (nextCursor, needDelay) = if (result.previous == null && cursor != null) {
            loader.seaportInfo("Previous cursor ($cursor) is not finalized, reuse it")
            cursor to true
        } else {
            val next = result.previous ?: result.next
            loader.seaportInfo("Use next cursor $next")
            next to false
        }
        openSeaFetchStateRepository.save(state.withCursor(nextCursor))
        if (result.orders.isEmpty() || needDelay) delay(properties.pollingPeriod)
    }

    private fun getDefaultFetchState(): OpenSeaFetchState {
        return OpenSeaFetchState(id = stateId, cursor = null, listedAfter = Instant.EPOCH.epochSecond)
    }

    private companion object {
        val loader: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
        const val STATE_ID_PREFIX = "seaport_order_fetch"
    }
}

