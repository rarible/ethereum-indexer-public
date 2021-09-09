package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.model.OpenSeaOrderItems
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.OrdersRequest
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

internal class OpenSeaOrderServiceTestUt {
    private val openSeaClient: OpenSeaClient = mockk()
    private val properties =  OrderListenerProperties(loadOpenSeaPeriod = Duration.ofSeconds(1))

    private val openSeaOrderService = OpenSeaOrderServiceImpl(openSeaClient, properties)

    @Test
    fun `should load batch correctly`() = runBlocking<Unit> {
        val listedAfter = 1L
        val listenerBefore = 5L

        val intervals = mutableSetOf(
            1L to 2L,
            2L to 3L,
            3L to 4L,
            4L to 5L
        )

        coEvery { openSeaClient.getOrders(any()) } answers {
            val request = it.invocation.args.first() as OrdersRequest
            val interval = request.listedAfter!!.epochSecond to request.listedBefore!!.epochSecond
            assertThat(intervals.remove(interval)).isTrue()

            OperationResult.Success(
                OpenSeaOrderItems(
                    count = 1,
                    orders = emptyList()
                )
            )
        }
        openSeaOrderService.getNextOrdersBatch(listedAfter, listenerBefore)
        coVerify (exactly = 4) { openSeaClient.getOrders(any()) }
    }
}
