package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.listener.configuration.UpdateSuspiciousItemsHandlerProperties
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.time.delay
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class UpdateSuspiciousItemsHandler(
    private val stateService: UpdateSuspiciousItemsStateService,
    private val suspiciousItemsService: SuspiciousItemsService,
    private val properties: UpdateSuspiciousItemsHandlerProperties,
) : JobHandler {

    override suspend fun handle() {
        val now = Instant.now()
        val state = stateService.getState() ?: stateService.getInitState(now)

        val newState = if (state.assets.isNotEmpty()) {
            update(state)
        } else {
            awaitNextStart(now, state)
        }
        stateService.save(newState)
    }

    private suspend fun update(state: UpdateSuspiciousItemsState) = coroutineScope {
        logger.info("Runnable state: {} assets to handle", state.assets.size)
        val assets = state.assets
            .chunked(properties.chunkSize)
            .map { chunk ->
                chunk
                    .map { asset -> async { suspiciousItemsService.update(asset) } }
                    .awaitAll()
                    .filter { asset -> asset.cursor != null }
            }
            .flatten()

        state.copy(assets = assets)
    }

    private suspend fun awaitNextStart(now: Instant, state: UpdateSuspiciousItemsState): UpdateSuspiciousItemsState {
        val previousStartTime = state.statedAt
        val nextStart = previousStartTime + properties.handlePeriod
        logger.info("No runnable state: nextStart={}, ", state.assets.size)

        return if (now >= nextStart) {
            stateService.getInitState(now)
        } else {
            val awaitDuration = minOf(properties.awakePeriod, Duration.between(nextStart, now))
            delay(awaitDuration)
            state
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(UpdateSuspiciousItemsHandler::class.java)
    }
}

