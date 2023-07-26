package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.common.ifNotBlank
import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.SeaportFetchState
import com.rarible.protocol.order.core.repository.opensea.OpenSeaFetchStateRepository
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.misc.seaportInfo
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SeaportOrderLoadHandler(
    private val seaportOrderLoader: SeaportOrderLoader,
    private val openSeaFetchStateRepository: OpenSeaFetchStateRepository,
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val properties: SeaportLoadProperties,
) : JobHandler {

    override suspend fun handle() {
        val state = aggregatorStateRepository.getSeaportState() ?: getDefaultFetchState()
        val cursor = state.cursor.ifNotBlank()
        val result = seaportOrderLoader.load(cursor, properties.asyncRequestsEnabled)

        val (nextCursor, needDelay) = if (result.previous == null && cursor != null) {
            logger.seaportInfo("Previous cursor ($cursor) is not finalized, reuse it")
            cursor to true
        } else {
            val next = result.previous ?: result.next ?: error("Can't determine next Seaport cursor")
            logger.seaportInfo("Use next cursor $next")
            next to false
        }
        aggregatorStateRepository.save(state.withCursor(nextCursor))
        if (result.orders.isEmpty() || needDelay) delay(properties.pollingPeriod)
    }

    private suspend fun getDefaultFetchState(): SeaportFetchState {
        val legacyState = openSeaFetchStateRepository
            .get(STATE_ID_PREFIX)?.cursor
            ?.let { SeaportFetchState(cursor = it) }

        return legacyState ?: SeaportFetchState(cursor = "")
    }

    internal companion object {

        val logger: Logger = LoggerFactory.getLogger(SeaportOrderLoadHandler::class.java)
        const val STATE_ID_PREFIX = "seaport_order_fetch"
    }
}
