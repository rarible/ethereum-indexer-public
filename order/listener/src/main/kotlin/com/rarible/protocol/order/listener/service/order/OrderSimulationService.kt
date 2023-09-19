package com.rarible.protocol.order.listener.service.order

import com.rarible.protocol.order.core.model.Order
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

    suspend fun simulate(order: Order): Boolean {
        return try {
            val hasCapacity = tenderlyService.hasCapacity()
            if (!hasCapacity) {
                // Send fake positive in case of reaching limit
                return true
            }

            val address = randomAddress() // buyer, could be any address
            val tx = transactionService.buyTx(order, address)
            val result = tenderlyService.simulate(tx)

            when {
                // Send fake positive in case of reaching limit
                result.reachLimit -> true

                // That means we don't reach request limit and order could be executed
                else -> result.status
            }
        } catch (ex: Exception) {
            logger.error("Simulation failed for order ${order.id}: ${ex.message}")
            false
        }
    }

    fun randomAddress(): Address {
        return ByteArray(20).let {
            SecureRandom().nextBytes(it)
            Address(it)
        }
    }
}
