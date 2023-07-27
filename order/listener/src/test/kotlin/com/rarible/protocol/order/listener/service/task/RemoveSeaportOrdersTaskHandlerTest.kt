package com.rarible.protocol.order.listener.service.task

import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.event.OrderListener
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderStateRepository
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

@ExperimentalCoroutinesApi
@FlowPreview
@IntegrationTest
class RemoveSeaportOrdersTaskHandlerTest : AbstractIntegrationTest() {
    private val orderListener = mockk<OrderListener>()

    @Autowired
    private lateinit var orderStateRepository: OrderStateRepository

    @BeforeEach
    fun setup() = runBlocking<Unit> {
        orderRepository.createIndexes()
    }

    @Test
    internal fun `remove order`() = runBlocking<Unit> {
        val now = Instant.now()
        val seaportAddress = randomAddress()
        val addresses = mockk<OrderIndexerProperties.ExchangeContractAddresses> {
            every { seaportV1_4 } returns seaportAddress
        }
        val properties = mockk<OrderIndexerProperties> {
            every { exchangeContractAddresses } returns addresses
            every { featureFlags } returns OrderIndexerProperties.FeatureFlags(removeOpenSeaOrdersInTask = true)
        }
        val handler = RemoveSeaportOrdersTaskHandler(
            orderRepository,
            orderVersionRepository,
            properties,
            orderStateRepository,
            orderUpdateService
        )
        val orderToRemove1 = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(1),
            data = createOrderBasicSeaportDataV1().copy(protocol = seaportAddress),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val otherOrder = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(2),
            data = createOrderBasicSeaportDataV1(),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val filledOrder = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(2),
            data = createOrderBasicSeaportDataV1(),
            fill = EthUInt256.ONE,
            status = OrderStatus.FILLED,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val orderToRemove2 = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(10),
            data = createOrderBasicSeaportDataV1().copy(protocol = seaportAddress),
            makeStock = EthUInt256.ZERO,
            status = OrderStatus.INACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val laterOrder = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(20),
            data = createOrderBasicSeaportDataV1().copy(protocol = randomAddress()),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        listOf(orderToRemove1, otherOrder, filledOrder, orderToRemove2, laterOrder).shuffled().forEach {
            orderRepository.save(it)
            orderVersionRepository.save(createOrderVersion().copy(hash = it.hash)).awaitFirst()
            orderStateRepository.save(OrderState(it.hash, true))
        }

        val result = handler.runLongTask(null, param = (now - Duration.ofMinutes(11)).epochSecond.toString()).toList()
        assertThat(result).isNotEmpty

        assertThat(orderRepository.findById(orderToRemove1.hash)).isNull()
        assertThat(orderVersionRepository.findAllByHash(orderToRemove1.hash).toList()).isEmpty()
        assertThat(orderStateRepository.getById(orderToRemove1.hash)).isNull()

        assertThat(orderRepository.findById(orderToRemove2.hash)).isNull()
        assertThat(orderVersionRepository.findAllByHash(orderToRemove2.hash).toList()).isEmpty()
        assertThat(orderStateRepository.getById(orderToRemove2.hash)).isNull()

        assertThat(orderRepository.findById(otherOrder.hash)).isNotNull
        assertThat(orderVersionRepository.findAllByHash(otherOrder.hash).toList()).isNotEmpty

        assertThat(orderRepository.findById(laterOrder.hash)).isNotNull
        assertThat(orderVersionRepository.findAllByHash(laterOrder.hash).toList()).isNotEmpty
    }

    @Test
    internal fun `change order state`() = runBlocking<Unit> {
        val now = Instant.now()
        val seaportAddress = randomAddress()
        val addresses = mockk<OrderIndexerProperties.ExchangeContractAddresses> {
            every { seaportV1_4 } returns seaportAddress
        }
        val properties = mockk<OrderIndexerProperties> {
            every { exchangeContractAddresses } returns addresses
            every { featureFlags } returns OrderIndexerProperties.FeatureFlags(removeOpenSeaOrdersInTask = false)
        }
        val handler = RemoveSeaportOrdersTaskHandler(
            orderRepository,
            orderVersionRepository,
            properties,
            orderStateRepository,
            orderUpdateService
        )
        val orderToRemove1 = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(1),
            data = createOrderBasicSeaportDataV1().copy(protocol = seaportAddress),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val otherOrder = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(2),
            data = createOrderBasicSeaportDataV1(),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val filledOrder = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(2),
            data = createOrderBasicSeaportDataV1(),
            fill = EthUInt256.ONE,
            status = OrderStatus.FILLED,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val orderToRemove2 = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(10),
            data = createOrderBasicSeaportDataV1().copy(protocol = seaportAddress),
            makeStock = EthUInt256.ZERO,
            status = OrderStatus.INACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        val laterOrder = randomOrder().copy(
            make = randomErc721(),
            lastUpdateAt = now - Duration.ofMinutes(20),
            data = createOrderBasicSeaportDataV1().copy(protocol = randomAddress()),
            status = OrderStatus.ACTIVE,
            type = OrderType.SEAPORT_V1,
            platform = Platform.OPEN_SEA,
        )
        listOf(orderToRemove1, otherOrder, filledOrder, orderToRemove2, laterOrder).shuffled().forEach {
            orderRepository.save(it)
            orderVersionRepository.save(createOrderVersion().copy(hash = it.hash)).awaitFirst()
        }
        coEvery {
            orderListener.onOrder(any(), any())
            orderListener.onOrder(any(), any())
        } returns Unit

        val result = handler.runLongTask(null, param = (now - Duration.ofMinutes(11)).epochSecond.toString()).toList()
        assertThat(result).isNotEmpty

        assertThat(orderRepository.findById(orderToRemove1.hash)?.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(orderRepository.findById(orderToRemove2.hash)?.status).isEqualTo(OrderStatus.CANCELLED)

        assertThat(orderRepository.findById(otherOrder.hash)?.status).isNotEqualTo(OrderStatus.CANCELLED)
        assertThat(orderRepository.findById(laterOrder.hash)?.status).isNotEqualTo(OrderStatus.CANCELLED)
    }
}
