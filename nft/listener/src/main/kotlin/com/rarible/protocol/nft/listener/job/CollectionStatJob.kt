package com.rarible.protocol.nft.listener.job

import com.rarible.core.apm.withTransaction
import com.rarible.protocol.nft.core.model.CollectionStat
import com.rarible.protocol.nft.core.repository.CollectionStatRepository
import com.rarible.protocol.nft.core.service.CollectionStatService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CollectionStatJob(
    private val collectionStatRepository: CollectionStatRepository,
    private val collectionStatService: CollectionStatService,
    @Value("\${listener.collectionStatRefresh.batchSize:20}")
    private val batchSize: Int,
    @Value("\${listener.collectionStatRefresh.timeOffset:PT1H}")
    private val timeOffset: Duration,
    @Value("\${listener.collectionStatRefresh.enabled:true}")
    private val enabled: Boolean
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedRateString = "\${listener.collectionStatRefresh.rate:PT1M}",
        initialDelayString = "PT1M"
    )
    fun execute() = runBlocking<Unit> {
        logger.info("Starting CollectionStatJob")
        if (enabled) {
            do {
                val updated = updateOld(batchSize, timeOffset)
                logger.info("Updated collection stats: {}", updated.size)
            } while (updated.isNotEmpty())
        }
    }

    private suspend fun updateOld(batchSize: Int, timeOffset: Duration): List<CollectionStat> {
        val oldStats = collectionStatRepository.findOld(batchSize, timeOffset)
        return coroutineScope {
            oldStats.map {
                async {
                    withTransaction(name = "updateCollectionStats", labels = listOf("collection" to it.id.prefixed())) {
                        collectionStatService.updateStat(it.id)
                    }
                }
            }.awaitAll()
        }
    }

}