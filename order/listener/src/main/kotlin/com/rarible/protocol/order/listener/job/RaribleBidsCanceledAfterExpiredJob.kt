package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.daonomic.rpc.domain.Word
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.time.delay
import java.time.Instant

class RaribleBidsCanceledAfterExpiredJob(
    private val orderRepository: OrderRepository,
    private val orderVersionRepository: OrderVersionRepository,
    private val orderUpdateService: OrderUpdateService,
    raribleOrderExpiration: OrderIndexerProperties.RaribleOrderExpirationProperties,
    properties: OrderListenerProperties,
    meterRegistry: MeterRegistry
) : SequentialDaemonWorker(
    meterRegistry,
    DaemonWorkerProperties(pollingPeriod = properties.raribleExpiredBidWorker.pollingPeriod)
) {
    private val delayPeriod = raribleOrderExpiration.delayPeriod
    private val bidExpirePeriod = raribleOrderExpiration.bidExpirePeriod

    override suspend fun handle() {
        val before = Instant.now() - bidExpirePeriod
        orderRepository.findAllLiveBidsHashesLastUpdatedBefore(before).collect {
            fixOrder(it)
            orderUpdateService.update(it)
            logger.info("Found expire bid $it after $before, bid is cancelled.")
        }
        delay(delayPeriod)
    }

    private suspend fun fixOrder(hash: Word) {
        val orderVersions = orderVersionRepository.findAllByHash(hash).toList()
        if (orderVersions.isEmpty()) {
            val order = orderRepository.findById(hash) ?: return
            val orderVersion = OrderVersion(
                    maker = order.maker,
                    make = order.make,
                    take = order.take,
                    taker = order.taker,
                    type = order.type,
                    salt = order.salt,
                    start = order.start,
                    end = order.end,
                    data = order.data,
                    signature = order.signature,
                    platform = order.platform,
                    hash = order.hash,
                    approved = order.approved,
                    createdAt = order.createdAt,
                    makePriceUsd = null,
                    takePriceUsd = null,
                    makePrice = null,
                    takePrice = null,
                    makeUsd = null,
                    takeUsd = null
            )
            orderVersionRepository.save(orderVersion).awaitFirst()
            logger.info("Fixed order $hash without version")
        }
    }
}
