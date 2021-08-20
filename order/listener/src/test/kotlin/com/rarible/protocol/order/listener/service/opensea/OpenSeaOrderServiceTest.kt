package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.OpenSeaClient
import com.rarible.opensea.client.model.OpenSeaOrder
import com.rarible.opensea.client.model.OpenSeaOrderItems
import com.rarible.opensea.client.model.OperationResult
import com.rarible.opensea.client.model.OrdersRequest
import com.rarible.protocol.order.listener.configuration.OrderListenerProperties
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration

internal class OpenSeaOrderServiceTestUt {
    private val openSeaClient: OpenSeaClient = mockk()
    private val properties =  OrderListenerProperties(loadOpenSeaPeriod = Duration.ofSeconds(1))

    private val openSeaOrderService = OpenSeaOrderService(openSeaClient, properties)

    @Test
    @Disabled
    fun `should load batch correctly`() = runBlocking<Unit> {
        val listedAfter = 1L
        val listenerBefore = 10L
        val orders = (0..9).map { mockk<OpenSeaOrder>() }

        coEvery { openSeaClient.getOrders(any()) } answers {
            val request = it.invocation.args.first() as OrdersRequest
            OperationResult.Success(
                OpenSeaOrderItems(
                    count = 1,
                    orders = listOf(
                        orders[request.listedAfter!!.epochSecond.toInt()]
                    )
                )
            )
        }
        openSeaOrderService.getNextOrdersBatch(listedAfter, listenerBefore)
    }
}
