package com.rarible.protocol.nft.listener.service.suspicios

import com.rarible.core.daemon.job.JobHandler
import com.rarible.protocol.nft.core.model.SuspiciousUpdateResult
import com.rarible.protocol.nft.core.model.UpdateSuspiciousItemsState
import com.rarible.protocol.nft.listener.configuration.UpdateSuspiciousItemsHandlerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
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
    private val listenerMetrics: NftListenerMetricsFactory,
) : JobHandler {

    override suspend fun handle() {
        val now = Instant.now()
        val state = getState(now)
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
                    .filter { result ->
                        when (result) {
                            is SuspiciousUpdateResult.Success -> result.next.cursor != null
                            is SuspiciousUpdateResult.Fail -> true
                        }
                    }
                    .map { result -> result.next }
            }
            .flatten()

        state.copy(assets = assets)
    }

    private suspend fun awaitNextStart(now: Instant, state: UpdateSuspiciousItemsState): UpdateSuspiciousItemsState {
        val previousStartTime = state.statedAt
        val nextStart = previousStartTime + properties.handlePeriod

        return if (now >= nextStart) {
            stateService.getInitState(now)
        } else {
            val awaitDuration = minOf(properties.awakePeriod, Duration.between(now, nextStart))
            logger.info("No runnable state: nextStart={}, await={}", nextStart, awaitDuration)
            delay(awaitDuration)
            state
        }
    }

    private suspend fun getState(now: Instant): UpdateSuspiciousItemsState {
        return (stateService.getState() ?: stateService.getInitState(now)).also {
            listenerMetrics.onSuspiciousCollectionsGet(it.assets.size)
        }
    }

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(UpdateSuspiciousItemsHandler::class.java)
    }
}
