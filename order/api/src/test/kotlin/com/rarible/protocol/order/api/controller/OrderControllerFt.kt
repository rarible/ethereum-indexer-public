package com.rarible.protocol.order.api.controller

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.contracts.exchange.v1.ExchangeV1
import com.rarible.protocol.contracts.exchange.v2.ExchangeV2
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.order.api.data.generateNewKeys
import com.rarible.protocol.order.api.data.toForm
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.misc.toWord
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.web.client.RestTemplate
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger
import com.rarible.protocol.order.api.data.createOrder as createOrderFully

@IntegrationTest
@Import(OrderControllerFt.TestOrderServiceConfiguration::class)
class OrderControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses

    @Autowired
    lateinit var orderService: OrderService

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    lateinit var restTemplate: RestTemplate

    /**
     * Test that "salt" field in the serialized OrderForm can be a big-integer, 0xHEX or a big-integer string.
     */
    @Test
    fun `salt is converted correctly from any string or numerical type`() = runBlocking {
        val salt = Numeric.toBigInt(RandomUtils.nextBytes(32))
        listOf<Any>(
            salt, /* as big int */
            //"\"" + salt.toWord() + "\"", /* as hex string of length 64 */
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
                    "http://localhost:${port}/v0.1/orders",
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

    private suspend fun testCreateOrderUsingPutRequest(
        salt: EthUInt256,
        upsert: suspend (OrderFormDto) -> OrderDto
    ) {
        val (privateKey, _, signer) = generateNewKeys()
        val order = createOrder(signer, Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN), salt)

        coEvery { orderService.put(any()) } returns order

        val formDto = order.toForm(EIP712Domain("", "", BigInteger.ONE, AddressFactory.create()), privateKey)

        val orderDto = upsert(formDto)
        assertThat((orderDto as RaribleV2OrderDto).make.value).isEqualTo(BigInteger.TEN)
        println(objectMapper.writeValueAsString(orderDto))
    }

    @Test
    fun `should handle correct request with empty result`() = runBlocking<Unit> {
        coEvery { orderService.findOrders(any(), any(), any(), any()) } returns emptyList()

        val result = orderClient.getSellOrders(null, null, null, null).awaitFirst()

        assertThat(result.orders).isEmpty()
    }

    @Test
    fun `should handle request with pagination`() = runBlocking<Unit> {
        val (_, _, user) = generateNewKeys()

        coEvery { orderService.findOrders(any(), any(), any(), any()) } returns listOf(
            createOrder(
                user,
                Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.ONE), EthUInt256.of(5)),
                EthUInt256.TEN
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

    @AfterEach
    fun afterEach() {
        clearMocks(orderService)
    }

    private fun createOrder(maker: Address, make: Asset, salt: EthUInt256) = Order(
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
        end = null,
        data = OrderRaribleV2DataV1(emptyList(), emptyList()),
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
