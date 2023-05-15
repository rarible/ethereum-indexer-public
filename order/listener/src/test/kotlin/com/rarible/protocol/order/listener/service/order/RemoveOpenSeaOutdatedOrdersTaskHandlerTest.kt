package com.rarible.protocol.order.listener.service.order

import com.rarible.core.test.data.randomInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrder
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderBasicSeaportDataV1
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.time.Instant

@FlowPreview
@IntegrationTest
class RemoveOpenSeaOutdatedOrdersTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var taskHandler: RemoveOpenSeaOutdatedOrdersTaskHandler

    @Test
    fun `remove OpenSea orders`() = runBlocking<Unit> {
        val openseaV1 = exchangeContractAddresses.openSeaV1
        val openseaV2 = exchangeContractAddresses.openSeaV2
        val seaportV1 = exchangeContractAddresses.seaportV1
        val seaportV1_4 = exchangeContractAddresses.seaportV1_4
        val seaportV1_5 = exchangeContractAddresses.seaportV1_5

        repeat(3) {
            orderRepository.save(createOrder().copy(status = OrderStatus.ACTIVE)).id
        }
        repeat(3) {
            orderRepository.save(createOrder().copy(status = OrderStatus.CANCELLED))
        }

        val seaportV1Active = createSeaportOrders(OrderStatus.ACTIVE, seaportV1)
        val seaportV1_4Active = createSeaportOrders(OrderStatus.ACTIVE, seaportV1_4)
        val seaportV1_5Active = createSeaportOrders(OrderStatus.ACTIVE, seaportV1_5)
        val seaportV1Cancelled = createSeaportOrders(OrderStatus.CANCELLED, seaportV1)

        val openseaV1Active = createOpenseaOrders(OrderStatus.ACTIVE, openseaV1)
        val openseaV2Active = createOpenseaOrders(OrderStatus.ACTIVE, openseaV2)
        val openseaV1Inactive = createOpenseaOrders(OrderStatus.INACTIVE, openseaV1)
        val openseaV2Inactive = createOpenseaOrders(OrderStatus.INACTIVE, openseaV2)
        val openseaV1NotStarted = createOpenseaOrders(OrderStatus.NOT_STARTED, openseaV1)
        val openseaV2NotStarted = createOpenseaOrders(OrderStatus.NOT_STARTED, openseaV2)

        val activeResult = taskHandler.runLongTask(null, OrderStatus.ACTIVE.name).toList()
        val inactiveResult = taskHandler.runLongTask(null, OrderStatus.INACTIVE.name).toList()
        val notStartedResult = taskHandler.runLongTask(null, OrderStatus.NOT_STARTED.name).toList()
        val result = activeResult + inactiveResult + notStartedResult

        val updated = orderRepository.findAll(result.map { Word.apply(it) }).toList()
        val updatedIds = updated.map { it.hash }

        assertThat(updatedIds).containsAll(openseaV1Active)
        assertThat(updatedIds).containsAll(openseaV2Active)
        assertThat(updatedIds).containsAll(openseaV1NotStarted)
        assertThat(updatedIds).containsAll(openseaV2NotStarted)
        assertThat(updatedIds).containsAll(openseaV1Inactive)
        assertThat(updatedIds).containsAll(openseaV2Inactive)
        assertThat(updatedIds).containsAll(seaportV1Active)
        assertThat(updatedIds).containsAll(seaportV1_4Active)
        assertThat(updatedIds).doesNotContainAnyElementsOf(seaportV1Cancelled)
        assertThat(updatedIds).doesNotContainAnyElementsOf(seaportV1_5Active)

        updated.forEach { assertEquals(true, it.cancelled) }
        updated.forEach { assertEquals(OrderStatus.CANCELLED, it.status) }

        updated.filter { it.data is OrderOpenSeaV1DataV1 }.forEach {
            when ((it.data as OrderOpenSeaV1DataV1).exchange) {
                openseaV1 -> assertThat(it.lastUpdateAt).isEqualTo(Instant.ofEpochSecond(1645812000))
                openseaV2 -> assertThat(it.lastUpdateAt).isEqualTo(Instant.ofEpochSecond(1659366000))
                else -> throw IllegalArgumentException("Unexpected OpenSea test exchange")
            }
        }

        updated.filter { it.data is OrderBasicSeaportDataV1 }.forEach {
            when ((it.data as OrderBasicSeaportDataV1).protocol) {
                seaportV1 -> assertThat(it.lastUpdateAt).isEqualTo(Instant.ofEpochSecond(1680652800))
                seaportV1_4 -> assertThat(it.lastUpdateAt).isEqualTo(Instant.ofEpochSecond(1684195200))
                else -> throw IllegalArgumentException("Unexpected Seaport test exchange")
            }
        }
    }

    private suspend fun createOpenseaOrders(status: OrderStatus, exchange: Address): List<Word> {
        return createOrders {
            val orderVersion = createOrderVersion().copy(
                platform = Platform.OPEN_SEA,
                data = createOrderOpenSeaV1DataV1().copy(exchange = exchange, nonce = 0),
                type = OrderType.OPEN_SEA_V1,
                createdAt = Instant.EPOCH,
            )
            orderVersionRepository.save(orderVersion).awaitFirst()
            orderVersion.toOrderExactFields().copy(
                status = status,
                cancelled = status == OrderStatus.CANCELLED
            )
        }
    }

    private suspend fun createSeaportOrders(status: OrderStatus, protocol: Address): List<Word> {
        return createOrders {
            val orderVersion = createOrderVersion().copy(
                platform = Platform.OPEN_SEA,
                data = createOrderBasicSeaportDataV1().copy(protocol = protocol, counterHex = EthUInt256.ZERO),
                type = OrderType.SEAPORT_V1,
                createdAt = Instant.EPOCH,
            )
            orderVersionRepository.save(orderVersion).awaitFirst()
            orderVersion.toOrderExactFields().copy(
                status = status,
                cancelled = status == OrderStatus.CANCELLED
            )
        }
    }

    private suspend fun createOrders(factory: suspend () -> Order): List<Word> {
        val count = randomInt(5) + 1
        return (1..count).map {
            val order = factory()
            orderRepository.save(order)
        }.map { it.hash }
    }
}
