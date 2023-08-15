package com.rarible.protocol.order.listener.service.looksrare

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.order.core.configuration.LooksrareLoadProperties
import com.rarible.protocol.order.core.model.LooksrareV2Cursor
import com.rarible.protocol.order.core.model.LooksrareV2State
import com.rarible.protocol.order.core.repository.state.AggregatorStateRepository
import kotlinx.coroutines.time.delay

abstract class LooksrareLoadHandler(
    private val aggregatorStateRepository: AggregatorStateRepository,
    private val properties: LooksrareLoadProperties
) : JobHandler {

    abstract suspend fun getState(): LooksrareV2State

    abstract suspend fun load(cursor: LooksrareV2Cursor): Result

    override suspend fun handle() {
        val state = getState()
        val cursor = state.looksrareV2Cursor
        val result = load(cursor)
        if (result.cursor != null) {
            aggregatorStateRepository.save(state.withCursor(result.cursor))
        }
        if (result.saved == 0L && result.cursor?.nextId == null) {
            delay(properties.pollingPeriod)
        }
    }
}
