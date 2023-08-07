package com.rarible.protocol.order.listener.service.opensea

import com.rarible.core.test.data.randomString
import com.rarible.opensea.client.SeaportProtocolClient
import com.rarible.opensea.client.model.OpenSeaError
import com.rarible.opensea.client.model.OpenSeaErrorCode
import com.rarible.opensea.client.model.OpenSeaResult
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.v2.OrdersRequest
import com.rarible.opensea.client.model.v2.SeaportOrders
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.listener.configuration.SeaportLoadProperties
import com.rarible.protocol.order.listener.data.randomSeaportOrder
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
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

    private val seaportRequestCursorProducer = mockk<SeaportRequestCursorProducer> {
        every { produceNextFromCursor(any(), any(), any()) } returns emptyList()
    }
    private val metrics: ForeignOrderMetrics = mockk {
        every { onOrderReceived(eq(Platform.OPEN_SEA), any()) } returns Unit
        every { onLatestOrderReceived(eq(Platform.OPEN_SEA), any()) } returns Unit
        every { onCallForeignOrderApi(eq(Platform.OPEN_SEA), any()) } returns Unit
    }
    private val seaportProtocolClient = mockk<SeaportProtocolClient>()
    private val seaportLoad = SeaportLoadProperties(retry = 2, retryDelay = Duration.ZERO)
    private val openSeaOrderService = OpenSeaOrderServiceImpl(
        seaportRequestCursorProducer = seaportRequestCursorProducer,
        seaportProtocolClient = seaportProtocolClient,
        seaportLoad = seaportLoad,
        metrics = metrics
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
        }) }
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
        }) }
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

    @Test
    fun `should handle multiply request`() = runBlocking<Unit> {
        seaportLoad.asyncRequestsEnabled = true
        seaportLoad.maxAsyncRequests = 3

        val cursor1 = randomString()
        val cursor2 = randomString()
        val cursor3 = randomString()

        val orders1 = listOf(randomSeaportOrder(), randomSeaportOrder())
        val orders2 = listOf(randomSeaportOrder(), randomSeaportOrder())
        val orders3 = listOf(randomSeaportOrder(), randomSeaportOrder())

        every {
            seaportRequestCursorProducer.produceNextFromCursor(cursor1, step = seaportLoad.loadMaxSize, amount = seaportLoad.maxAsyncRequests - 1)
        } returns listOf(cursor2, cursor3)

        coEvery {
            seaportProtocolClient.getListOrders(
                OrdersRequest(
                    cursor = cursor1,
                    limit = seaportLoad.loadMaxSize
                )
            )
        } returns OpenSeaResult.success(SeaportOrders(next = "next1", previous = "previous1", orders = orders1))
        coEvery {
            seaportProtocolClient.getListOrders(
                OrdersRequest(
                    cursor = cursor2,
                    limit = seaportLoad.loadMaxSize
                )
            )
        } returns OpenSeaResult.success(SeaportOrders(next = "next12", previous = "previous2", orders = orders2))
        coEvery {
            seaportProtocolClient.getListOrders(
                OrdersRequest(
                    cursor = cursor3,
                    limit = seaportLoad.loadMaxSize
                )
            )
        } returns OpenSeaResult.success(SeaportOrders(next = "next3", previous = "previous3", orders = orders3))

        val result = openSeaOrderService.getNextSellOrders(cursor1, loadAhead = true)
        assertThat(result.orders).isEqualTo(orders1 + orders2 + orders3)
        assertThat(result.next).isEqualTo("next3")
        assertThat(result.previous).isEqualTo("previous3")
    }

    @Test
    fun `should handle get last result with not null previous`() = runBlocking<Unit> {
        seaportLoad.asyncRequestsEnabled = true
        seaportLoad.maxAsyncRequests = 3

        val cursor1 = randomString()
        val cursor2 = randomString()

        val orders1 = listOf(randomSeaportOrder(), randomSeaportOrder())
        val orders2 = listOf(randomSeaportOrder(), randomSeaportOrder())

        every {
            seaportRequestCursorProducer.produceNextFromCursor(cursor1, step = seaportLoad.loadMaxSize, amount = seaportLoad.maxAsyncRequests - 1)
        } returns listOf(cursor2)

        coEvery {
            seaportProtocolClient.getListOrders(
                OrdersRequest(
                    cursor = cursor1,
                    limit = seaportLoad.loadMaxSize
                )
            )
        } returns OpenSeaResult.success(SeaportOrders(next = "next1", previous = "previous1", orders = orders1))
        coEvery {
            seaportProtocolClient.getListOrders(
                OrdersRequest(
                    cursor = cursor2,
                    limit = seaportLoad.loadMaxSize
                )
            )
        } returns OpenSeaResult.success(SeaportOrders(next = "next12", previous = null, orders = orders2))

        val result = openSeaOrderService.getNextSellOrders(cursor1, loadAhead = true)
        assertThat(result.orders).isEqualTo(orders1 + orders2)
        assertThat(result.next).isEqualTo("next1")
        assertThat(result.previous).isEqualTo("previous1")
    }
}
