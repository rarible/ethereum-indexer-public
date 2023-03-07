package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV1
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV1
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.listener.data.createOrder
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderCounterMigrationTaskHandlerIt {

    @Autowired
    lateinit var orderRepository: OrderRepository

    @Autowired
    lateinit var taskHandler: OrderCounterMigrationTaskHandler

    @Test
    fun test() = runBlocking<Unit> {
        // Should be updated
        val seaportData = createOrderBasicSeaportDataV1().copy(counterHex = null)
        val seaportOrder = orderRepository.save(createOrder().copy(data = seaportData))

        // Should be updated
        val lrData = createOrderLooksrareDataV1().copy(counterHex = null)
        val lrOrder = orderRepository.save(createOrder().copy(data = lrData))

        // Skipped - hex counter already set
        val alreadyUpdatedData = createOrderBasicSeaportDataV1()
        val alreadyUpdatedOrder = orderRepository.save(createOrder().copy(data = alreadyUpdatedData))

        // Skipped - hex counter already set
        val otherData = createOrderRaribleV2DataV1()
        val otherOrder = orderRepository.save(createOrder().copy(data = otherData))

        taskHandler.runLongTask(null, "").collect()

        val updatedOpenSea = orderRepository.findById(seaportOrder.hash)!!
        assertThat(updatedOpenSea.version).isEqualTo(1L)
        assertThat(updatedOpenSea.data).isEqualTo(seaportData.copy(counterHex = EthUInt256.of(seaportData.counter!!)))

        val updatedLooksrare = orderRepository.findById(lrOrder.hash)!!
        assertThat(updatedLooksrare.version).isEqualTo(1L)
        assertThat(updatedLooksrare.data).isEqualTo(lrData.copy(counterHex = EthUInt256.of(lrData.counter!!)))

        assertThat(alreadyUpdatedOrder).isEqualTo(orderRepository.findById(alreadyUpdatedOrder.hash))
        assertThat(otherOrder).isEqualTo(orderRepository.findById(otherOrder.hash))
    }

}