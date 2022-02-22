package com.rarible.protocol.nft.listener.job

import com.rarible.protocol.nft.core.service.CollectionStatService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CollectionStatJob(
    private val collectionStatService: CollectionStatService,
    @Value("\${listener.collectionStatRefresh.batchSize:20}")
    private val batchSize: Int,
    @Value("\${listener.collectionStatRefresh.timeOffset:P1H}")
    private val timeOffset: Duration
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedRateString = "\${listener.collectionStatRefresh.rate:P1M}",
        initialDelayString = "P1M"
    )
    suspend fun execute() {
        logger.info("Starting CollectionStatJob")
        do {
            val updated = collectionStatService.updateOld(batchSize, timeOffset)
            logger.info("Updated collection stats: {}", updated.size)
        } while (updated.isNotEmpty())
    }

}