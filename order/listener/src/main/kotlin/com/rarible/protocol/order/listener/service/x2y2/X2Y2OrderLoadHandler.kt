package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.X2Y2FetchState
import com.rarible.protocol.order.core.repository.x2y2.X2Y2FetchStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2LoadProperties
import com.rarible.protocol.order.listener.misc.x2y2Info
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

class X2Y2OrderLoadHandler(
    private val stateRepository: X2Y2FetchStateRepository,
    private val x2y2OrderLoader: X2Y2OrderLoader,
    private val properties: X2Y2LoadProperties
) : JobHandler {

    override suspend fun handle() {
        val cursor = stateRepository.get(X2Y2FetchState.ID)?.cursor ?: getDefaultStartCursor()
        val result = x2y2OrderLoader.load(cursor)

        val next = result.next
        val (nextCursor, needDelay) = if (next == null) {
            loader.x2y2Info("Previous cursor ($cursor) is not finalized, reuse it")
            cursor to false
        } else {
            loader.x2y2Info("Use next cursor $next")
            next to false
        }
        stateRepository.save(X2Y2FetchState.withCursor(nextCursor))
        if (result.data.isEmpty() || needDelay) delay(properties.pollingPeriod)
    }

    private fun getDefaultStartCursor(): String {
        return (properties.startCursor ?: Instant.now().toEpochMilli())
            .let { Base64.getEncoder().encodeToString("[${it}]".toByteArray()) }
    }

    private companion object {
        val loader: Logger = LoggerFactory.getLogger(X2Y2OrderLoadHandler::class.java)
    }
}