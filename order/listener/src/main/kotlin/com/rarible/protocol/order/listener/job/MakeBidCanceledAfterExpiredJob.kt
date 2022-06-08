package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import io.micrometer.core.instrument.MeterRegistry
import java.time.Instant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import org.springframework.stereotype.Component

@Component
class MakeBidCanceledAfterExpiredJob(
    private val orderRepository: OrderRepository,
    private val orderReduceService: OrderReduceService,
    properties: OrderIndexerProperties.ExpiredBidWorker,
    meterRegistry: MeterRegistry
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties(pollingPeriod = properties.pollingPeriod)
) {

    private val expirePeriod = properties.raribleBidExpirePeriod

    override suspend fun handle() {
        try {

            val expired =
                orderRepository.findAllLiveBidsHashesLastUpdatedBefore(Instant.now() - expirePeriod)
            expired.collect {
                orderReduceService.update(it).asFlow().collect { bid ->
                    logger.info("Expire bid $bid after $expirePeriod, bid is cancelled now!")
                }
            }
        } catch (e: Exception) {
            logger.error("Unable to handle expired bids: ${e.message}", e)
            throw e
        }
    }


}
