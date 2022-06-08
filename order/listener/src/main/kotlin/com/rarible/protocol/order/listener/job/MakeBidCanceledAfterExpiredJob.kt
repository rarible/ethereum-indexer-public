package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderReduceService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.micrometer.core.instrument.MeterRegistry
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MakeBidCanceledAfterExpiredJob(
    private val orderRepository: OrderRepository,
    @Value("\${listener.raribleBidExpirePeriod}")
    private val expirePeriod: Duration,
    private val orderReduceService: OrderReduceService,
    properties: OrderListenerProperties,
    meterRegistry: MeterRegistry
): SequentialDaemonWorker(meterRegistry, properties.expireBidWorker) {

    override suspend fun handle() {
        try {
            val expired = orderRepository.findAllLiveBidsHashesLastUpdatedBefore(Instant.now() - expirePeriod)
            expired.collect {
                orderReduceService.update(it).asFlow().collect {bid ->
                    logger.info("Expire bid $bid after $expirePeriod, bid is cancelled now!")
                }
            }
        } catch (e: Exception) {
            logger.error("Unable to handle expired bids: ${e.message}",  e)
            throw e
        }
    }


}
