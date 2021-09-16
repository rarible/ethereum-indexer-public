package com.rarible.protocol.order.listener.service.event

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.*
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.service.asset.AssetBalanceProvider
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.awaitFirst
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import scalether.domain.AddressFactory
import java.time.Duration
import java.util.*
import java.util.stream.Stream

@FlowPreview
@IntegrationTest
@Import(OrderUpdateConsumerEventHandlerTest.TestPriceUpdateService::class)
internal class OrderUpdateConsumerEventHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    protected lateinit var orderVersionRepository: OrderVersionRepository

    @Autowired
    private lateinit var priceUpdateService: PriceUpdateService

    @Autowired
    private lateinit var assetMakeBalanceProvider: AssetMakeBalanceProvider

    companion object {
        @JvmStatic
        fun nftItemOrders(): Stream<Arguments> = run {
            val token = AddressFactory.create()
            val toneId = EthUInt256.TEN

            Stream.of(
                Arguments.arguments(
                    OrderKind.SELL,
                    createOrderVersion().copy(
                        make = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN),
                        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
                    ),
                    (1..10).map {
                        createOrderVersion().copy(
                            make = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN),
                            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN)
                        )
                    }
                ),
                Arguments.arguments(
                    OrderKind.BID,
                    createOrderVersion().copy(
                        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                        take = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN)
                    ),
                    (1..10).map {
                        createOrderVersion().copy(
                            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                            take = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN)
                        )
                    }
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("nftItemOrders")
    fun handleOrderUpdateEvent(
        kind: OrderKind,
        nftOrderVersion: OrderVersion,
        nftOrderVersions: List<OrderVersion>
    ) = runBlocking {
        val producer = RaribleKafkaProducer(
            clientId = "update-price-update-"+UUID.randomUUID(),
            valueSerializerClass = JsonSerializer::class.java,
            valueClass = OrderUpdateEventDto::class.java,
            defaultTopic = OrderIndexerTopicProvider.getUpdateTopic(
                application.name,
                orderIndexerProperties.blockchain.value
            ),
            bootstrapServers = orderIndexerProperties.kafkaReplicaSet
        )
        val nftOrder = save(nftOrderVersion)
        val nftOrders = nftOrderVersions.map { save(it) }

        val newOrderUsd = setRandomPriceUpdateServiceMock(kind)

        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = nftOrder.hash.toString(),
            order = OrderDtoConverter.convert(nftOrder)
        )
        val sendJob = async {
            val message = KafkaMessage(
                key = "test",
                id = UUID.randomUUID().toString(),
                value = event,
                headers = emptyMap()
            )
            producer.send(message)
        }
        Wait.waitAssert(Duration.ofSeconds(10)) {
            nftOrders.map { it.hash }.forEach { hash ->
                orderVersionRepository.findAllByHash(hash).collect { orderVersion ->
                    checkOrderVersionPrices(orderVersion, newOrderUsd)
                }

                val order = orderRepository.findById(hash) ?: error("Can't find test order $hash")
                checkOrderPrices(order, newOrderUsd)
            }
        }
        sendJob.cancelAndJoin()
    }

    private suspend fun save(orderVersion: OrderVersion): Order {
        val order = orderVersion.toOrderExactFields().copy(hash = orderVersion.hash)
        assertThat(order.hash).isEqualTo(orderVersion.hash)

        orderVersionRepository.save(orderVersion).awaitFirst()
        return orderRepository.save(order)
    }

    private suspend fun setRandomPriceUpdateServiceMock(kind: OrderKind): OrderUsdValue {
        val orderUsd = when (kind) {
            OrderKind.SELL -> OrderUsdValue.SellOrder(takeUsd = randomBigDecimal(), makePriceUsd = randomBigDecimal())
            OrderKind.BID -> OrderUsdValue.BidOrder(makeUsd = randomBigDecimal(), takePriceUsd = randomBigDecimal())
        }
        coEvery { priceUpdateService.getAssetsUsdValue(make = any(), take = any(), at = any()) } returns orderUsd
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.ZERO
        return orderUsd
    }

    private fun checkOrderPrices(order: Order, orderUsd: OrderUsdValue) {
        assertThat(order.makeUsd).isEqualTo(orderUsd.makeUsd)
        assertThat(order.takeUsd).isEqualTo(orderUsd.takeUsd)
        assertThat(order.takePriceUsd).isEqualTo(orderUsd.takePriceUsd)
        assertThat(order.makePriceUsd).isEqualTo(orderUsd.makePriceUsd)
    }

    private fun checkOrderVersionPrices(orderVersion: OrderVersion, orderUsd: OrderUsdValue) {
        assertThat(orderVersion.makeUsd).isEqualTo(orderUsd.makeUsd)
        assertThat(orderVersion.takeUsd).isEqualTo(orderUsd.takeUsd)
        assertThat(orderVersion.takePriceUsd).isEqualTo(orderUsd.takePriceUsd)
        assertThat(orderVersion.makePriceUsd).isEqualTo(orderUsd.makePriceUsd)
    }

    @TestConfiguration
    class TestPriceUpdateService {
        @Bean
        @Primary
        fun mockkPriceUpdateService(): PriceUpdateService {
            return mockk()
        }
    }
}
