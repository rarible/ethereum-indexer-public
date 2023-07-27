package com.rarible.protocol.nft.listener.job

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FixStandardJob(
    listenerProps: NftListenerProperties,
    private val tokenRepository: TokenRepository,
    private val tokenService: TokenService,
    private val reindexTokenService: ReindexTokenService,
    metricsFactory: NftListenerMetricsFactory,
) {

    private val props = listenerProps.fixStandardJob
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fixedCounter = metricsFactory.tokenStandardJobFixedbCounter
    private val unfixedCounter = metricsFactory.tokenStandardJobUnfixedCounter

    @Scheduled(
        fixedDelayString = "\${listener.fixStandardJob.rate:PT30M}",
        initialDelayString = "PT1M"
    )
    fun execute() = runBlocking<Unit> {
        logger.info("Starting FixStandardJob")
        if (props.enabled) {
            val found = tokenRepository.findNone(props.batchSize, props.retries)
            logger.info("Found ${found.size} tokens with NONE standard")
            // we get address of token only if it is changed standard
            val addresses = found.mapNotNull { token ->
                val updated = tokenService.update(token.id)
                incrementMetric(updated)
                tokenRepository.incrementRetry(token.id)
                if (updated?.standard?.isNotIgnorable() == true) {
                    updated.id
                } else {
                    null
                }
            }
            if (addresses.isNotEmpty()) {
                reindexTokenService.createReindexAndReduceTokenTasks(addresses)
            } else {
                logger.info("There are no non-ignorable tokens in the fixed list")
            }
        }
    }

    suspend fun incrementMetric(updated: Token?) {
        if (updated?.standard?.isNotIgnorable() == true) {
            fixedCounter.increment()
            logger.info("Token ${updated.id} changed standard to ${updated.standard}")
        } else {
            unfixedCounter.increment()
        }
    }
}
