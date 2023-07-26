package com.rarible.protocol.order.listener.service.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.test.data.randomInt
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderDataVersion
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.toOrderExactFields
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.shaded.org.apache.commons.lang.time.DateUtils
import scalether.domain.Address
import java.time.Instant

@IntegrationTest
internal class RemoveOutdatedOrdersTaskHandlerTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var removeOutdatedOrdersTaskHandler: RemoveOutdatedOrdersTaskHandler

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `remove OpenSea orders`() = runBlocking<Unit> {
        val openseaV1 = exchangeContractAddresses.openSeaV1
        val openseaV2 = exchangeContractAddresses.openSeaV2
        val seaportV1 = exchangeContractAddresses.seaportV1
        val seaportV1_4 = exchangeContractAddresses.seaportV1_4
        val seaportV1_5 = exchangeContractAddresses.seaportV1_5

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

        val result = removeOutdatedOrdersTaskHandler.runLongTask(
            from = null,
            param = objectMapper.writeValueAsString(
                RemoveOutdatedOrdersTaskParams(
                    platform = Platform.OPEN_SEA,
                    status = OrderStatus.ACTIVE,
                    type = OrderType.OPEN_SEA_V1,
                    version = OrderDataVersion.OPEN_SEA_V1_DATA_V1,
                    contractAddress = openseaV1.toString()
                )
            )
        ).toList()
        assertThat(result).containsExactlyInAnyOrder(*openseaV1Active.map { it.toString() }.toTypedArray())

        openseaV1Active.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.CANCELLED)
        }
        openseaV2Active.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.ACTIVE)
        }
        openseaV1Inactive.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.INACTIVE)
        }
        openseaV2Inactive.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.INACTIVE)
        }
        openseaV1NotStarted.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.NOT_STARTED)
        }
        openseaV2NotStarted.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.NOT_STARTED)
        }
        seaportV1Active.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.ACTIVE)
        }
        seaportV1_4Active.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.ACTIVE)
        }
        seaportV1_5Active.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.ACTIVE)
        }
        seaportV1Cancelled.forEach {
            assertThat(orderRepository.findById(it)!!.status).isEqualTo(OrderStatus.CANCELLED)
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
                start = if (status == OrderStatus.NOT_STARTED) {
                    System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY
                } else {
                    null
                },
                approved = status !== OrderStatus.INACTIVE,
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
