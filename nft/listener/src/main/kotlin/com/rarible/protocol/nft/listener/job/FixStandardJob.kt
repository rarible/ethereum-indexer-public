package com.rarible.protocol.nft.listener.job

import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.metrics.NftListenerMetricsFactory
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class FixStandardJob(
    @Value("\${listener.fixStandard.batchSize:20}")
    private val batchSize: Int,
    @Value("\${listener.fixStandard.enabled:true}")
    private val enabled: Boolean,
    @Value("\${listener.fixStandard.retries:5}")
    private val retries: Int,
    private val tokenRepository: TokenRepository,
    private val tokenRegistrationService: TokenRegistrationService,
    private val reindexTokenService: ReindexTokenService,
    metricsFactory: NftListenerMetricsFactory,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val fixedCounter = metricsFactory.tokenStandardJobFixedbCounter
    private val unfixedCounter = metricsFactory.tokenStandardJobUnfixedCounter

    @Scheduled(
        fixedDelayString = "\${listener.fixStandard.rate:PT15M}",
        initialDelayString = "PT1M"
    )
    fun execute() = runBlocking<Unit> {
        logger.info("Starting FixStandardJob")
        if (enabled) {
            do {
                val found = tokenRepository.findNone(batchSize, retries)
                logger.info("Found ${found.size} tokens with NONE standard")
                // we get address of token only if it is changed standard
                val addresses = found.mapNotNull { token ->
                    val updated = tokenRegistrationService.update(token.id)
                    val result = if (updated?.standard in setOf(TokenStandard.ERC721, TokenStandard.ERC1155)) {
                        fixedCounter.increment()
                        updated?.id
                    } else {
                        unfixedCounter.increment()
                        null
                    }
                    incrementRetry(updated ?: token)
                    result
                }
                reindexTokenService.createReindexTokenTask(addresses)
            } while (found.isNotEmpty())
        }
    }

    suspend fun incrementRetry(token: Token) {
        val current = token.standardRetries ?: 0
        tokenRepository.save(token.copy(standardRetries = current + 1)).awaitSingle()
    }
}
