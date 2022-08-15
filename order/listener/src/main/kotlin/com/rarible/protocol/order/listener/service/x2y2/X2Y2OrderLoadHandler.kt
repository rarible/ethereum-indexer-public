package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.X2Y2FetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2LoadProperties
import com.rarible.protocol.order.listener.misc.x2y2Info
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class X2Y2OrderLoadHandler(
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val x2y2OrderLoader: X2Y2OrderLoader,
    private val properties: X2Y2LoadProperties
) : JobHandler {

    override suspend fun handle() {
        val state = aggregatorStateRepository.getX2Y2State() ?: getDefaultFetchState()
        val result = x2y2OrderLoader.load(state.cursor)

        val next = result.next
        val (nextCursor, needDelay) = if (next == null) {
            loader.x2y2Info("Previous cursor (${state.cursor}) is not finalized, reuse it")
            state.cursor to true
        } else {
            loader.x2y2Info("Use next cursor $next")
            next to false
        }
        aggregatorStateRepository.save(state.withCursor(nextCursor))
        if (result.data.isEmpty() || needDelay) delay(properties.pollingPeriod)
    }

    private fun getDefaultFetchState(): X2Y2FetchState {
        return X2Y2FetchState(
            cursor = (properties.startCursor ?: Instant.now().toEpochMilli()).let { Base64.getEncoder().encodeToString("[${it}]".toByteArray()) }
        )
    }

    private companion object {
        val loader: Logger = LoggerFactory.getLogger(X2Y2OrderLoadHandler::class.java)
    }
}