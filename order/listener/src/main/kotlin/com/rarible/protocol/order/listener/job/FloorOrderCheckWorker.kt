package com.rarible.protocol.order.listener.job

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.repository.TopCollectionRepository
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.validator.OrderValidator
import com.rarible.protocol.order.listener.configuration.FloorOrderCheckWorkerProperties
import com.rarible.protocol.order.listener.service.order.OrderSimulationService
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import scalether.domain.Address
import java.util.concurrent.atomic.AtomicInteger

@Component
@ConditionalOnProperty(name = ["listener.floorOrderCheckWorker.enabled"], havingValue = "true")
class FloorOrderCheckWorker(
    properties: FloorOrderCheckWorkerProperties,
    meterRegistry: MeterRegistry,
    private val handler: FloorOrderCheckHandler
) : SequentialDaemonWorker(
    meterRegistry = meterRegistry,
    properties = DaemonWorkerProperties().copy(
        pollingPeriod = properties.pollingPeriod,
        errorDelay = properties.errorDelay
    ),
    workerName = "floor-bid-check-job"
) {

    override suspend fun handle() = handler.handle()

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationStarted() = start()
}

@Component
class FloorOrderCheckHandler(
    private val orderRepository: OrderRepository,
    private val coreOrderValidator: OrderValidator,
    private val topCollectionProvider: TopCollectionProvider,
    private val orderSimulation: OrderSimulationService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val chunkSize = 10

    suspend fun handle() = coroutineScope {
        val topCollections = topCollectionProvider.getTopCollections()
        logger.info("Starting to check floor sell Orders for {} top Collections", topCollections.size)
        topCollections.chunked(chunkSize)
            .forEach { chunk ->
                chunk.map {
                    async { checkCollection(it) }
                }.awaitAll()
            }
        logger.info("Finished checking floor sell Orders for {} top Collections", topCollections.size)
    }

    private suspend fun checkCollection(token: Address) {
        val start = System.currentTimeMillis()
        val currencies = orderRepository.findActiveSellCurrenciesByCollection(token)
        if (currencies.isEmpty()) {
            logger.info("No active Orders found for Collection {}", token.prefixed())
            return
        }

        val invalid = currencies.sumOf { checkSellFloorOrder(token, it) }

        logger.info(
            "Floor Orders check finished for Collection {}, {} invalid Orders found ({})",
            token.prefixed(),
            invalid,
            System.currentTimeMillis() - start
        )
    }

    private suspend fun checkSellFloorOrder(token: Address, currency: Address): Int {
        logger.info("Checking floor Orders for Collection {} (currency = {})", token.prefixed(), currency.prefixed())
        val invalidCounter = AtomicInteger()
        do {
            val bestOrder = orderRepository.findActiveBestSellOrdersOfCollection(token, currency, 1)
                .firstOrNull()

            val validFound = when (bestOrder) {
                null -> true
                else -> {
                    val isValid = checkOrder(bestOrder)
                    if (!isValid) invalidCounter.incrementAndGet()
                    isValid
                }
            }
        } while (!validFound)

        return invalidCounter.get()
    }

    private suspend fun checkOrder(order: Order): Boolean {
        val valid = try {
            coreOrderValidator.validate(order)
            true
        } catch (e: Exception) {
            logger.info("Order {} ({}) validation failed: {}", order.hash.prefixed(), order.platform, e.message)
            false
        }
        return if (valid && orderSimulation.isEnabled) {
            orderSimulation.simulate(order)
        } else {
            valid
        }
    }
}

@Component
class TopCollectionProvider(
    private val topCollectionRepository: TopCollectionRepository
) {
    // TODO replace it in case of other top collections source appears
    suspend fun getTopCollections() = topCollectionRepository.getAll()
}
