package com.rarible.protocol.order.api.controller

import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.SyncSortDto
import com.rarible.protocol.order.api.data.createLogEvent
import com.rarible.protocol.order.api.data.orderErc1155SellCancel
import com.rarible.protocol.order.api.data.orderErc1155SellSideMatch
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.model.OrderSide
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class OrderActivityControllerIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var controller: OrderActivityController

    @BeforeEach
    fun createIndexes() = runBlocking<Unit> {
        exchangeHistoryRepository.createIndexes()
    }

    @Test
    fun `should get all needed history activities - asc`() = runBlocking<Unit> {
        val size = 10
        val activity1 = createLogEvent(orderErc1155SellSideMatch().copy(side = OrderSide.LEFT))
        val activity2 = createLogEvent(orderErc1155SellSideMatch().copy(side = OrderSide.RIGHT))
        val activity3 = createLogEvent(orderErc1155SellCancel())
        listOf(activity1, activity2, activity3).forEach {
            exchangeHistoryRepository.save(it).awaitFirst()
        }
        val dto = controller.getOrderActivitiesSync(null, size, SyncSortDto.DB_UPDATE_ASC)
        val orderList = dto.body?.items

        Assertions.assertThat(orderList).hasSize(2)

        Assertions.assertThat(orderList?.map { it.id })
            .containsExactlyInAnyOrder(activity1.id, activity3.id)
    }

    @Test
    fun `should get all right history activities - asc`() = runBlocking<Unit> {
        val size = 10
        val activity1 = createLogEvent(orderErc1155SellSideMatch().copy(side = OrderSide.LEFT))
        val activity2 = createLogEvent(orderErc1155SellSideMatch().copy(side = OrderSide.RIGHT))
        val activity3 = createLogEvent(orderErc1155SellSideMatch().copy(side = OrderSide.RIGHT))
        val activity4 = createLogEvent(orderErc1155SellCancel())
        listOf(activity1, activity2, activity3, activity4).forEach {
            exchangeHistoryRepository.save(it).awaitFirst()
        }
        val dto = controller.getOrderSellRightActivities(null, size)
        val idsList = dto.body?.items

        Assertions.assertThat(idsList).hasSize(2)

        Assertions.assertThat(idsList).containsExactlyInAnyOrder(activity2.id, activity3.id)
    }

    @Test
    fun `should get history activities - asc`() = runBlocking<Unit> {
        val ordersQuantity = 20 // must be even
        val size = 20

        repeat(ordersQuantity) {
            exchangeHistoryRepository.save(createLogEvent(orderErc1155SellSideMatch())).awaitFirst()
            exchangeHistoryRepository.save(createLogEvent(orderErc1155SellCancel().copy(maker = null))).awaitFirst()
            exchangeHistoryRepository.save(createLogEvent(orderErc1155SellCancel().copy(make = null))).awaitFirst()
            exchangeHistoryRepository.save(createLogEvent(orderErc1155SellCancel().copy(take = null))).awaitFirst()
        }

        val dto = controller.getOrderActivitiesSync(null, size, SyncSortDto.DB_UPDATE_ASC)
        val orderList = dto.body?.items

        Assertions.assertThat(orderList).hasSize(size)
        Assertions.assertThat(orderList)
            .isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt, o1.lastUpdatedAt) }
    }

    @Test
    fun `should get all order activities using pagination desc`() = runBlocking<Unit> {
        val ordersQuantity = 30 // must be even
        val ordersChunk = 5

        repeat(ordersQuantity / 2) {
            val historySave = exchangeHistoryRepository.save(createLogEvent(orderErc1155SellSideMatch()))
            val versionSave = orderVersionRepository.save(createOrderVersion())
            historySave.awaitFirst()
            versionSave.awaitFirst()
        }

        var continuation: String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderActivityDto>()

        do {
            val dto = controller.getOrderActivitiesSync(continuation, ordersChunk, SyncSortDto.DB_UPDATE_DESC)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(ordersQuantity / ordersChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(ordersQuantity)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o2.lastUpdatedAt, o1.lastUpdatedAt) }
    }

    @Test
    fun `should get all order activities using pagination asc`() = runBlocking<Unit> {
        val ordersQuantity = 30 // must be even
        val ordersChunk = 5

        repeat(ordersQuantity / 2) {
            val historySave = exchangeHistoryRepository.save(createLogEvent(orderErc1155SellSideMatch()))
            val versionSave = orderVersionRepository.save(createOrderVersion())
            historySave.awaitFirst()
            versionSave.awaitFirst()
        }

        var continuation: String? = null
        var pageCounter = 0
        val receivedOrders = mutableListOf<OrderActivityDto>()

        do {
            val dto = controller.getOrderActivitiesSync(continuation, ordersChunk, SyncSortDto.DB_UPDATE_ASC)
            continuation = dto.body?.continuation
            dto.body?.let { receivedOrders.addAll(it.items) }
            pageCounter += 1
        } while (continuation != null)

        Assertions.assertThat(pageCounter).isEqualTo(ordersQuantity / ordersChunk + 1)
        Assertions.assertThat(receivedOrders).hasSize(ordersQuantity)
        Assertions.assertThat(receivedOrders)
            .isSortedAccordingTo { o1, o2 -> compareValues(o1.lastUpdatedAt, o2.lastUpdatedAt) }
    }
}
