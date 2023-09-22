package com.rarible.protocol.order.listener.service.order

import com.rarible.ethereum.domain.Blockchain
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.api.ApiClient
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.BuyTx
import com.rarible.protocol.order.core.model.Erc1155AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.OrderRaribleV2DataV2
import com.rarible.protocol.order.core.model.Part
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.configuration.TxBackendProperties
import com.rarible.protocol.order.listener.data.createOrder
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

class OrderTransactionServiceTest {

    val mapper = ApiClient.createDefaultObjectMapper()
    val mockServer = MockWebServer()
    val common: OrderIndexerProperties = mockk() {
        every { blockchain } returns Blockchain.ETHEREUM
    }
    val props = OrderListenerProperties(
        txBackendProperties = TxBackendProperties(url = "http://localhost:${mockServer.port}")
    )
    val service = OrderTransactionService(common, props)

    @Test
    fun `get tx - ok`() = runBlocking<Unit> {
        val mockedResult = BuyTx("", "", BigInteger.valueOf(10100000000), "0x0d5f7d3500000")
        mockServer.enqueue(mockOkResponse(mockedResult))

        val address = Address.apply("0xa3b2d9ffe53b509f917f47f7531f15d73b99b913")
        val order = createOrder(
            token = Address.apply("0xd07dc4262bcdbf85190c01c996b4c06a461d2430"),
            tokenId = EthUInt256.Companion.of(285235)).copy(
                hash = Word.apply("0x95c6c6c1d9fcaf18b78e70215fbf3c4934645de0c1ae1881a366d4b3092bcffb"),
                maker = Address.apply("0x051b064efa5cf67c75e17c8eae1f1326bf1ce43e"),
                make = Asset(Erc1155AssetType(
                    token = Address.apply("0xd07dc4262bcdbf85190c01c996b4c06a461d2430"),
                    tokenId = EthUInt256.Companion.of(570855L)
                ), EthUInt256.ONE),
                take = Asset(EthAssetType, EthUInt256.Companion.of(2000000000000000000)),
                salt = EthUInt256.of("0x142f8298481d26ede1af6c8d761a5cd5d3d40d6cce09148f9bf9ea83ee66b765"),
                data = OrderRaribleV2DataV2(
                    payouts = emptyList(),
                    originFees = listOf(Part(Address.apply("0x1cf0df2a5a20cd61d68d4489eebbf85b8d39e18a"), value = EthUInt256.Companion.of(100))),
                    isMakeFill = true
                )
            )
        val data = service.buyTx(order, address)
        assertThat(data).isEqualTo(mockedResult)

        // check request
        assertThat(mockServer.requestCount).isEqualTo(1)
        assertThat(mockServer.takeRequest().path).isEqualTo("/v0.1/orders/buy-tx")
    }

    @Test
    fun `get tx - fail with 5xx`() = runBlocking<Unit> {
        mockServer.enqueue(MockResponse().setResponseCode(500))

        val address = AddressFactory.create()
        val order = createOrder()
        assertThrows<RuntimeException> {
            service.buyTx(order, address)
        }
    }

    private fun mockOkResponse(result: BuyTx): MockResponse {
        return MockResponse()
            .setBody(mapper.writeValueAsString(result))
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
    }
}
