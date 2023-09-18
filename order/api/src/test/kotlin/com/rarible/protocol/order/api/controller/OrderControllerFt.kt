package com.rarible.protocol.order.api.controller

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.RaribleV2OrderFormDto
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Buy
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Sell
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV1
import com.rarible.protocol.order.core.data.toForm
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderData
import com.rarible.protocol.order.core.model.OrderDataLegacy
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.OrderType
import io.daonomic.rpc.domain.Word
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.web3jold.utils.Numeric
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.time.Instant
import com.rarible.protocol.order.core.data.randomOrder as createOrderFully

@IntegrationTest
@Import(OrderControllerFt.TestOrderServiceConfiguration::class)
class OrderControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var orderService: OrderService

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var restTemplate: RestTemplate

    private val eip712Domain = EIP712Domain("test", "1", BigInteger.ONE, Address.ZERO())

    /**
     * Test that "salt" field in the serialized OrderForm can be a big-integer, 0xHEX or a big-integer string.
     */
    @Test
    fun `salt is converted correctly from any string or numerical type`() = runBlocking {
        val salt = Numeric.toBigInt(RandomUtils.nextBytes(32))
        listOf<Any>(
            salt, /* as big int */
            // "\"" + salt.toWord() + "\"", /* as hex string of length 64 */
            "\"" + salt + "\"" /* as a decimal string */
        ).forEach { saltFormat ->
            testCreateOrderUsingPutRequest(EthUInt256(salt)) { orderForm ->
                val headers = HttpHeaders()
                headers.contentType = MediaType.APPLICATION_JSON
                val modifiedJson = jacksonObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValueAsString(orderForm).lines().joinToString("\n") { line ->
                        if (line.contains("\"salt\"")) "\"salt\": $saltFormat," else line
                    }
                val httpRequest = HttpEntity(modifiedJson, headers)
                val orderDto = restTemplate.postForObject(
                    "http://localhost:$port/v0.1/orders",
                    httpRequest,
                    OrderDto::class.java
                )!!
                Assert.assertEquals(salt.toWord(), orderDto.salt)
                orderDto
            }
        }
    }

    @Test
    fun `should create order using PUT request`() = runBlocking {
        testCreateOrderUsingPutRequest(EthUInt256.TEN) { orderForm ->
            orderClient.upsertOrder(orderForm).awaitFirst()
        }
    }

    @Test
    fun `should create order with data v3 sell using PUT request`() = runBlocking {
        val salt = EthUInt256.TEN
        val data = createOrderRaribleV1DataV3Sell()

        testCreateOrderUsingPutRequest(salt, data) { orderForm ->
            val orderDto = orderClient.upsertOrder(orderForm).awaitFirst() as RaribleV2OrderDto
            assertThat(orderDto.data).isInstanceOf(OrderRaribleV2DataV3SellDto::class.java)
            orderDto
        }
    }

    @Test
    fun `should create order with data v3 buy using PUT request`() = runBlocking {
        val salt = EthUInt256.TEN
        val data = createOrderRaribleV1DataV3Buy()

        testCreateOrderUsingPutRequest(salt, data) { orderForm ->
            val orderDto = orderClient.upsertOrder(orderForm).awaitFirst() as RaribleV2OrderDto
            assertThat(orderDto.data).isInstanceOf(OrderRaribleV2DataV3BuyDto::class.java)
            orderDto
        }
    }

    private suspend fun testCreateOrderUsingPutRequest(
        salt: EthUInt256,
        data: OrderData = createOrderRaribleV2DataV1(),
        upsert: suspend (OrderFormDto) -> OrderDto
    ) {
        val (privateKey, _, signer) = generateNewKeys()
        val order = createOrder(
            maker = signer,
            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
            salt = salt,
            data = data
        )

        coEvery { orderService.put(any()) } returns order

        val formDto = order.toForm(eip712Domain, privateKey)

        val orderDto = upsert(formDto)
        assertThat((orderDto as RaribleV2OrderDto).make.value).isEqualTo(BigInteger.TEN)
        println(objectMapper.writeValueAsString(orderDto))
    }

    @Test
    fun `should handle correct request with empty result`() = runBlocking<Unit> {
        coEvery { orderService.findOrders(any(), any(), any()) } returns emptyList()

        val result = orderClient.getSellOrders(null, null, null, null).awaitFirst()

        assertThat(result.orders).isEmpty()
    }

    @Test
    fun `should handle request with pagination`() = runBlocking<Unit> {
        val (_, _, user) = generateNewKeys()

        coEvery { orderService.findOrders(any(), any(), any()) } returns listOf(
            createOrder(
                maker = user,
                make = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.ONE), EthUInt256.of(5)),
                salt = EthUInt256.TEN
            )
        )

        val continuation = Continuation.LastDate(nowMillis(), Word.apply(ByteArray(32)))

        val result = orderClient.getSellOrders(null, null, continuation.toString(), null).awaitFirst()

        assertThat(result.orders).hasSize(1)

        val orderDto = result.orders.first()
        assertThat(result.continuation).isNull()
    }

    @Test
    fun `should prepare order cancel tx for v1`() = runBlocking<Unit> {
        val order = createOrderFully().copy(type = OrderType.RARIBLE_V1, data = OrderDataLegacy(100))
        coEvery { orderService.get(eq(order.hash)) } returns order

        val result = orderClient.prepareOrderCancelTransaction(order.hash.toString()).awaitFirst()
        assertThat(result.to).isEqualTo(exchangeContractAddresses.v1)
        assertThat(result.data).isEqualTo(ExchangeV1.cancelSignature().encode(order.forV1Tx()._1()))
    }

    @Test
    fun `should prepare order cancel tx for v2`() = runBlocking<Unit> {
        val order =
            createOrderFully().copy(type = OrderType.RARIBLE_V2, data = OrderRaribleV2DataV1(emptyList(), emptyList()))
        coEvery { orderService.get(eq(order.hash)) } returns order

        val result = orderClient.prepareOrderCancelTransaction(order.hash.toString()).awaitFirst()
        assertThat(result.to).isEqualTo(exchangeContractAddresses.v2)
        assertThat(result.data).isEqualTo(ExchangeV2.cancelSignature().encode(order.forTx()))
    }

    @Test
    fun `invalid id format`() {
        assertThatExceptionOfType(OrderControllerApi.ErrorGetSellOrdersByCollectionAndByStatus::class.java).isThrownBy {
            orderClient.getSellOrdersByCollectionAndByStatus(
                "ETHEREUM:0x59325733eb952a92e069c87f0a6168b29e80627f",
                null,
                null,
                null,
                1000,
                null
            ).block()
        }.withMessageContaining("400 Bad Request")
    }

    @Test
    fun `should create order with null end - fail`() {
        val (privateKey, _, signer) = generateNewKeys()
        val order = createOrder(
            maker = signer,
            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
            salt = EthUInt256.TEN,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            endDate = null
        )
        val formDto = order.toForm(eip712Domain, privateKey)
        val request: MutableMap<String, Object> = objectMapper.convertValue<Map<String, Object>>(formDto).toMutableMap()
        request.remove("end")
        assertThatExceptionOfType(HttpClientErrorException.BadRequest::class.java).isThrownBy {
            restTemplate.postForObject(
                "http://localhost:$port/v0.1/orders",
                request,
                OrderDto::class.java
            )
        }.withMessageContaining("Missed end date")
    }

    @Test
    fun `should create order with invalid end - fail`() {
        val (privateKey, _, signer) = generateNewKeys()
        val order = createOrder(
            maker = signer,
            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
            salt = EthUInt256.TEN,
            data = OrderRaribleV2DataV1(emptyList(), emptyList())
        )
        val formDto = order.toForm(eip712Domain, privateKey) as RaribleV2OrderFormDto
        assertThatExceptionOfType(OrderControllerApi.ErrorUpsertOrder::class.java).isThrownBy {
            orderClient.upsertOrder(
                formDto.copy(
                    start = nowMillis().plusSeconds(1000).epochSecond,
                    end = nowMillis().epochSecond
                )
            ).block()
        }.withMessageContaining("Bad Request")
    }

    @Test
    fun `should create order with invalid signature - fail`() {
        val (privateKey, _, signer) = generateNewKeys()
        val order = createOrder(
            maker = AddressFactory.create(),
            make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN),
            salt = EthUInt256.TEN,
            data = OrderRaribleV2DataV1(emptyList(), emptyList())
        )
        val formDto = order.toForm(eip712Domain, privateKey) as RaribleV2OrderFormDto
        assertThatExceptionOfType(OrderControllerApi.ErrorUpsertOrder::class.java).isThrownBy {
            orderClient.upsertOrder(
                formDto.copy(
                    start = nowMillis().plusSeconds(1000).epochSecond,
                    end = nowMillis().epochSecond
                )
            ).block()
        }.withMessageContaining("Bad Request")
    }

    @AfterEach
    fun afterEach() {
        clearMocks(orderService)
    }

    private fun createOrder(
        maker: Address,
        make: Asset,
        salt: EthUInt256,
        data: OrderData = OrderRaribleV2DataV1(emptyList(), emptyList()),
        endDate: Instant? = Instant.MAX
    ) = Order(
        maker = maker,
        taker = null,
        make = make,
        take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5)),
        makeStock = make.value,
        type = OrderType.RARIBLE_V2,
        fill = EthUInt256.ZERO,
        cancelled = false,
        salt = salt,
        start = null,
        end = endDate?.epochSecond,
        data = data,
        signature = null,
        createdAt = nowMillis(),
        lastUpdateAt = nowMillis()
    )

    @TestConfiguration
    class TestOrderServiceConfiguration {
        @Bean
        @Primary
        fun mockedOrderService(): OrderService {
            return mockk()
        }
    }
}
