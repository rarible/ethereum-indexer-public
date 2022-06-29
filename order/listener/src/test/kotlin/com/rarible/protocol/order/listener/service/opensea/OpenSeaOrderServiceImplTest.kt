package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.test.data.randomString
import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.SeaportProtocolClient
import com.rarible.opensea.client.model.OpenSeaError
import com.rarible.opensea.client.model.OpenSeaErrorCode
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

internal class OpenSeaOrderServiceImplTest {
    private val openSeaClient = mockk<OpenSeaClient>()
    private val seaportProtocolClient = mockk<SeaportProtocolClient>()
    private val properties = mockk<OrderListenerProperties> {
        every { seaportLoad } returns SeaportLoadProperties(retry = 2, retryDelay = Duration.ZERO)
        every { openSeaOrderSide } returns OrderListenerProperties.OrderSide.SELL
    }
    private val openSeaOrderService = OpenSeaOrderServiceImpl(
        seaportProtocolClient = seaportProtocolClient,
        openSeaClient = openSeaClient,
        properties = properties
    )

    @Test
    fun `should load seaport orders`() = runBlocking<Unit> {
        val next = randomString()
        val expectedResult = SeaportOrders(next = "", previous = null, orders = emptyList())

        coEvery { seaportProtocolClient.getListOrders(any()) } returns OperationResult.success(expectedResult)

        val result = openSeaOrderService.getNextSellOrders(next)

        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 1) { seaportProtocolClient.getListOrders(withArg {
            assertThat(it.cursor).isEqualTo(next)
        })}
    }

    @Test
    fun `should retry load after fail`() = runBlocking<Unit> {
        val next = randomString()
        val expectedResult = SeaportOrders(next = "", previous = null, orders = emptyList())

        coEvery { seaportProtocolClient.getListOrders(any()) }
            .returns(OperationResult.fail(OpenSeaError(500, OpenSeaErrorCode.SERVER_ERROR, "")))
            .andThen(OperationResult.success(expectedResult))

        val result = openSeaOrderService.getNextSellOrders(next)

        assertThat(result).isEqualTo(expectedResult)
        coVerify(exactly = 2) { seaportProtocolClient.getListOrders(withArg {
            assertThat(it.cursor).isEqualTo(next)
        })}
    }

    @Test
    fun `should fail load after all retries fails`() = runBlocking<Unit> {
        val next = randomString()

        coEvery { seaportProtocolClient.getListOrders(any()) }
            .returns(OperationResult.fail(OpenSeaError(500, OpenSeaErrorCode.SERVER_ERROR, "error")))

        assertThrows<IllegalStateException> {
            runBlocking {
                openSeaOrderService.getNextSellOrders(next)
            }
        }
    }
}