package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.withTransaction
import com.rarible.protocol.order.core.model.CollectionOrderStat
import com.rarible.protocol.order.core.repository.CollectionOrderStatRepository
import com.rarible.protocol.order.core.service.CollectionOrderStatService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CollectionOrderStatJob(
    private val collectionOrderStatRepository: CollectionOrderStatRepository,
    private val collectionOrderStatService: CollectionOrderStatService,
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
        logger.info("Starting CollectionOrderStatJob")
        do {
            val updated = updateOld(batchSize, timeOffset)
            logger.info("Updated collection order stats: {}", updated.size)
        } while (updated.isNotEmpty())
    }

    private suspend fun updateOld(batchSize: Int, timeOffset: Duration): List<CollectionOrderStat> {
        val oldStats = collectionOrderStatRepository.findOld(batchSize, timeOffset)
        return coroutineScope {
            oldStats.map {
                async {
                    withTransaction(
                        name = "updateCollectionOrderStats",
                        labels = listOf("collection" to it.id.prefixed())
                    ) {
                        collectionOrderStatService.updateStat(it.id)
                    }
                }
            }.awaitAll()
        }
    }

}