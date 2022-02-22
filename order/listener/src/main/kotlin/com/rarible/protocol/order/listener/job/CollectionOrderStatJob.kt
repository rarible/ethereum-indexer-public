package com.rarible.protocol.order.listener.job

import com.rarible.core.apm.withTransaction
import com.rarible.protocol.order.core.model.CollectionOrderStat
import com.rarible.protocol.order.core.repository.CollectionOrderStatRepository
import com.rarible.protocol.order.core.service.CollectionOrderStatService
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
class CollectionOrderStatJob(
    private val collectionOrderStatRepository: CollectionOrderStatRepository,
    private val collectionOrderStatService: CollectionOrderStatService,
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
        logger.info("Starting CollectionOrderStatJob")
        if (enabled) {
            do {
                val updated = updateOld(batchSize, timeOffset)
                logger.info("Updated collection order stats: {}", updated.size)
            } while (updated.isNotEmpty())
        }
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