package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.order.OrderSimulation
import com.rarible.protocol.order.listener.configuration.FloorOrderCheckWorkerProperties
import com.rarible.protocol.order.listener.service.tenderly.TenderlyService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import scalether.domain.Address
import java.security.SecureRandom

@Service
class OrderSimulationService(
    private val properties: FloorOrderCheckWorkerProperties,
    private val transactionService: OrderTransactionService,
    private val tenderlyService: TenderlyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    val isEnabled get() = properties.simulateFloorOrders

    suspend fun simulate(order: Order): OrderSimulation {
        logger.info("Starting order simulation ${order.id}")
        return try {
            if (!tenderlyService.hasCapacity()) {
                // Send fake positive in case of reaching limit
                return OrderSimulation.REACHED_LIMIT
            }

            logger.info("Checked request limit for ${order.id}")
            val address = randomAddress() // buyer, could be any address
            val tx = transactionService.buyTx(order, address)
            logger.info("Got buy-tx for ${order.id}: $tx")
            val result = tenderlyService.simulate(tx)

            when {
                // Send fake positive in case of reaching limit
                result.reachLimit -> OrderSimulation.REACHED_LIMIT

                // That means we don't reach request limit and order could be executed
                result.status -> OrderSimulation.SUCCESS
                !result.status -> OrderSimulation.FAIL
                else -> OrderSimulation.ERROR
            }
        } catch (ex: Exception) {
            logger.error("Simulation failed for order ${order.id} (${order.platform.name}): ${ex.message}")
            OrderSimulation.ERROR
        } finally {
            logger.info("Simulation finished for ${order.id}")
        }
    }

    fun randomAddress(): Address {
        return ByteArray(20).let {
            SecureRandom().nextBytes(it)
            Address(it)
        }
    }
}
