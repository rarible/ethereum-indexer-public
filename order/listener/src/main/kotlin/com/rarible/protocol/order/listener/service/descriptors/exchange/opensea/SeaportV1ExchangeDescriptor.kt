package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.contracts.seaport.v1.events.OrderFulfilledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.listener.misc.ForeignOrderMetrics
import com.rarible.protocol.order.listener.service.descriptors.AutoReduceService
import com.rarible.protocol.order.listener.service.descriptors.ExchangeSubscriber
import com.rarible.protocol.order.listener.service.opensea.SeaportEventConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class SeaportV1ExchangeDescriptor(
    contractsProvider: ContractsProvider,
    private val seaportEventConverter: SeaportEventConverter,
    private val metrics: ForeignOrderMetrics,
    private val featureFlags: OrderIndexerProperties.FeatureFlags,
    autoReduceService: AutoReduceService,
) : ExchangeSubscriber<OrderSideMatch>(
    name = "os_fulfilled",
    topic = OrderFulfilledEvent.id(),
    contracts = contractsProvider.seaportV1(),
    autoReduceService = autoReduceService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(
        log: Log,
        transaction: Transaction,
        timestamp: Instant,
        index: Int,
        totalLogs: Int
    ): List<OrderSideMatch> {
        val event = OrderFulfilledEvent.apply(log)
        val orderSideMatches = seaportEventConverter.convert(event, timestamp, transaction.input())
        recordMetric(orderSideMatches, log)
        return markEvent(orderSideMatches, event, index, totalLogs, transaction)
    }

    private suspend fun markEvent(
        sideMatched: List<OrderSideMatch>,
        event: OrderFulfilledEvent,
        index: Int,
        totalLogs: Int,
        transaction: Transaction
    ): List<OrderSideMatch> {
        val adhoc = seaportEventConverter.isAdhocOrderEvent(event, index, totalLogs, transaction)
        logger.info("Event ${event.log().logIndex()} in tx=${event.log().transactionHash()} is adhoc: $adhoc")
        return if (adhoc) {
            if (featureFlags.markIgnoredEvent) sideMatched.map { it.copy(ignoredEvent = true) } else emptyList()
        } else sideMatched
    }

    private fun recordMetric(sideMatches: List<OrderSideMatch>, log: Log) {
        if (sideMatches.isEmpty()) {
            logger.warn("Can't convert event $log to order side matches")
            metrics.onOrderEventError(Platform.OPEN_SEA, "match", "mismatch")
        } else {
            metrics.onOrderEventHandled(Platform.OPEN_SEA, "match")
        }
    }
}
