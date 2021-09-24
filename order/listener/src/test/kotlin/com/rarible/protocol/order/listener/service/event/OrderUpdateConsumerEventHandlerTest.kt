package com.rarible.protocol.order.listener.service.event

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.kafka.json.JsonSerializer
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.dto.OrderIndexerTopicProvider
import com.rarible.protocol.dto.OrderUpdateEventDto
import com.rarible.protocol.order.core.converters.dto.OrderDtoConverter
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.repository.order.OrderVersionRepository
import com.rarible.protocol.order.listener.data.createOrderVersion
import com.rarible.protocol.order.listener.integration.AbstractIntegrationTest
import com.rarible.protocol.order.listener.integration.IntegrationTest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.time.Duration
import java.util.*
import java.util.stream.Stream

@FlowPreview
@IntegrationTest
internal class OrderUpdateConsumerEventHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var application: ApplicationEnvironmentInfo

    @Autowired
    protected lateinit var orderVersionRepository: OrderVersionRepository

    @Autowired
    protected lateinit var currencyControllerApi: CurrencyControllerApi

    @Autowired
    protected lateinit var orderDtoConverter: OrderDtoConverter

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
                        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                        takeUsd = null,
                        makeUsd = null,
                        takePriceUsd = null,
                        makePriceUsd = null
                    ),
                    (1..10).map {
                        createOrderVersion().copy(
                            make = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN),
                            take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                            takeUsd = null,
                            makeUsd = null,
                            takePriceUsd = null,
                            makePriceUsd = null
                        )
                    }
                ),
                Arguments.arguments(
                    OrderKind.BID,
                    createOrderVersion().copy(
                        make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                        take = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN),
                        takeUsd = null,
                        makeUsd = null,
                        takePriceUsd = null,
                        makePriceUsd = null
                    ),
                    (1..10).map {
                        createOrderVersion().copy(
                            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
                            take = Asset(Erc1155AssetType(token, toneId), EthUInt256.TEN),
                            takeUsd = null,
                            makeUsd = null,
                            takePriceUsd = null,
                            makePriceUsd = null
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

        val event = OrderUpdateEventDto(
            eventId = UUID.randomUUID().toString(),
            orderId = nftOrder.hash.toString(),
            order = orderDtoConverter.convert(nftOrder)
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
                    checkOrderVersionPrices(orderVersion, kind)
                }

                val order = orderRepository.findById(hash) ?: error("Can't find test order $hash")
                checkOrderPrices(order, kind)
            }
        }
        sendJob.cancelAndJoin()
    }

    private suspend fun save(orderVersion: OrderVersion): Order {
        return orderUpdateService.save(orderVersion)
    }

    private fun checkOrderPrices(order: Order, kind: OrderKind) {
        when (kind) {
            OrderKind.SELL -> {
                assertThat(order.takeUsd).isNotNull()
                assertThat(order.makePriceUsd).isNotNull()
                assertThat(order.makeUsd).isNull()
                assertThat(order.takePriceUsd).isNull()
            }
            OrderKind.BID -> {
                assertThat(order.makeUsd).isNotNull()
                assertThat(order.takePriceUsd).isNotNull()
                assertThat(order.takeUsd).isNull()
                assertThat(order.makePriceUsd).isNull()
            }
        }
    }

    private fun checkOrderVersionPrices(orderVersion: OrderVersion, kind: OrderKind) {
        when (kind) {
            OrderKind.SELL -> {
                assertThat(orderVersion.takeUsd).isNotNull()
                assertThat(orderVersion.makePriceUsd).isNotNull()
                assertThat(orderVersion.makeUsd).isNull()
                assertThat(orderVersion.takePriceUsd).isNull()
            }
            OrderKind.BID -> {
                assertThat(orderVersion.makeUsd).isNotNull()
                assertThat(orderVersion.takePriceUsd).isNotNull()
                assertThat(orderVersion.takeUsd).isNull()
                assertThat(orderVersion.makePriceUsd).isNull()
            }
        }
    }
}
