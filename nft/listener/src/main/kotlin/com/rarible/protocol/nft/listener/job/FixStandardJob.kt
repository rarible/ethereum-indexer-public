package com.rarible.protocol.nft.listener.job

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FixStandardJob(
    listenerProps: NftListenerProperties,
    private val tokenRepository: TokenRepository,
    private val tokenRegistrationService: TokenRegistrationService,
    private val reindexTokenService: ReindexTokenService,
    metricsFactory: NftListenerMetricsFactory,
) {

    private val props = listenerProps.fixStandardJob
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fixedCounter = metricsFactory.tokenStandardJobFixedbCounter
    private val unfixedCounter = metricsFactory.tokenStandardJobUnfixedCounter

    @Scheduled(
        fixedDelayString = "\${listener.fixStandard.rate:PT30M}",
        initialDelayString = "PT1M"
    )
    fun execute() = runBlocking<Unit> {
        logger.info("Starting FixStandardJob")
        var remains = props.reindexLimit
        if (props.enabled) {
            do {
                val found = tokenRepository.findNone(props.batchSize, props.retries)
                logger.info("Found ${found.size} tokens with NONE standard")
                // we get address of token only if it is changed standard
                val addresses = found.mapNotNull { token ->
                    val updated = tokenRegistrationService.update(token.id)
                    incrementMetric(updated)
                    incrementRetry(updated ?: token)
                    if (updated?.standard?.isNotIgnorable() == true) {
                        updated?.id
                    } else {
                        null
                    }
                }
                if (addresses.isNotEmpty()) {
                    reindexTokenService.createReindexAndReduceTokenTasks(addresses)
                    remains--
                } else {
                    logger.info("There are no non-ignorable tokens in the fixed list")
                }
            } while (found.isNotEmpty() && remains > 0)
        }
    }

    suspend fun incrementRetry(token: Token) {
        val current = token.standardRetries ?: 0
        tokenRepository.save(token.copy(standardRetries = current + 1)).awaitSingle()
    }

    suspend fun incrementMetric(updated: Token?) {
        if (updated?.standard?.isNotIgnorable() == true) {
            fixedCounter.increment()
            logger.info("Token ${updated?.id} changed standard to ${updated?.standard}")
        } else {
            unfixedCounter.increment()
        }
    }
}