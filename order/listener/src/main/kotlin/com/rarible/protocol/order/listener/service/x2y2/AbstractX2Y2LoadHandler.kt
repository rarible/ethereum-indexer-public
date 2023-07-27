package com.rarible.protocol.order.listener.service.x2y2

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.model.AggregatorFetchState
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import com.rarible.protocol.order.listener.configuration.X2Y2LoadProperties
import com.rarible.protocol.order.listener.misc.x2y2Info
import com.rarible.x2y2.client.model.ApiListResponse
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Base64

abstract class AbstractX2Y2LoadHandler<T>(
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val properties: X2Y2LoadProperties
) : JobHandler {
    protected val logger: Logger = LoggerFactory.getLogger(javaClass)

    protected abstract suspend fun getState(): AggregatorFetchState?

    protected abstract suspend fun getResult(cursor: String): ApiListResponse<T>

    protected abstract fun getDefaultFetchState(): AggregatorFetchState

    override suspend fun handle() {
        val state = getState() ?: getDefaultFetchState()
        val result = getResult(state.cursor)
        val next = result.next
        val (nextCursor, needDelay) = if (next == null) {
            logger.x2y2Info("Previous cursor (${state.cursor}) is not finalized, reuse it")
            state.cursor to true
        } else {
            logger.x2y2Info("Use next cursor $next")
            next to false
        }
        aggregatorStateRepository.save(state.withCursor(nextCursor))
        if (result.data.isEmpty() || needDelay) delay(properties.pollingPeriod)
    }

    protected fun codeCursor(cursor: Long): String {
        return Base64.getEncoder().encodeToString("[$cursor]".toByteArray())
    }
}
