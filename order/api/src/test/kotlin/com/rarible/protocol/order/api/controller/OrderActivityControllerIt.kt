package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.OrderActivitiesSyncTypesDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.api.data.createLogEvent
import com.rarible.protocol.order.api.data.orderErc1155SellSideMatch
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.createOrderVersion
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderActivityControllerIt: AbstractIntegrationTest()  {

    @Autowired
    private lateinit var controller: OrderActivityController

    @Test
    fun `should get all order activities using pagination desc`() = runBlocking<Unit> {
        val ordersQuantity = 30 //must be even
        val ordersChunk = 5

        repeat(ordersQuantity/2) {
            val historySave = exchangeHistoryRepository.save(createLogEvent(orderErc1155SellSideMatch()))
            val versionSave = orderVersionRepository.save(createOrderVersion())
            historySave.awaitFirst()
            versionSave.awaitFirst()
        }

        var continuation : String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderActivityDto>()

        do {
            val dto = controller.getOrderActivitiesSync(continuation, ordersChunk, SyncSortDto.DB_UPDATE_DESC, null)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(ordersQuantity/ordersChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(ordersQuantity)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt, o1.lastUpdatedAt) }
    }

    @Test
    fun `should get all order activities using pagination asc`() = runBlocking<Unit> {
        val ordersQuantity = 30 //must be even
        val ordersChunk = 5

        repeat(ordersQuantity/2) {
            val historySave = exchangeHistoryRepository.save(createLogEvent(orderErc1155SellSideMatch()))
            val versionSave = orderVersionRepository.save(createOrderVersion())
            historySave.awaitFirst()
            versionSave.awaitFirst()
        }

        var continuation : String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderActivityDto>()

        do {
            val dto = controller.getOrderActivitiesSync(continuation, ordersChunk, SyncSortDto.DB_UPDATE_ASC, null)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(ordersQuantity/ordersChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(ordersQuantity)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o1.lastUpdatedAt, o2.lastUpdatedAt) }
    }

    @Test
    fun `should get all order activities of certain types using pagination desc`() = runBlocking<Unit> {
        val ordersQuantity = 30 //must be even
        val ordersChunk = 5

        repeat(ordersQuantity/2) {
            val historySave = exchangeHistoryRepository.save(createLogEvent(orderErc1155SellSideMatch()))
            val versionSave = orderVersionRepository.save(createOrderVersion())
            historySave.awaitFirst()
            versionSave.awaitFirst()
        }

        var continuation : String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderActivityDto>()
        val filter = listOf(OrderActivitiesSyncTypesDto.MATCH)

        do {
            val dto = controller.getOrderActivitiesSync(continuation, ordersChunk, SyncSortDto.DB_UPDATE_DESC, filter)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(((ordersQuantity/2)/ordersChunk)+1)
        Assertions.assertThat(receivedOrders).hasSize(ordersQuantity/2)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt, o1.lastUpdatedAt) }
    }

}