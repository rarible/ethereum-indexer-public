package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.time.delay

class RaribleBidsCanceledAfterExpiredJob(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    raribleOrderExpiration: OrderIndexerProperties.RaribleOrderExpirationProperties,
    properties: OrderListenerProperties,
    meterRegistry: MeterRegistry
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties(pollingPeriod = properties.raribleExpiredBidWorker.pollingPeriod)
) {
    private val delayPeriod = raribleOrderExpiration.delayPeriod
    private val fixedExpireDate = raribleOrderExpiration.fixedExpireDate

    override suspend fun handle() {
        orderRepository.findAllLiveBidsHashesLastUpdatedBefore(fixedExpireDate).collect {
            logger.info("Found expire bid $it after $fixedExpireDate, bid is cancelled.")
            orderUpdateService.update(it)
            logger.info("Bid $it updated")
        }
        delay(delayPeriod)
    }
}
