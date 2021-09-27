package com.rarible.protocol.nftorder.listener.service

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.nftorder.core.converter.ShortOrderConverter
import com.rarible.protocol.nftorder.core.data.Fetched
import com.rarible.protocol.nftorder.core.event.OwnershipEventListener
import com.rarible.protocol.nftorder.core.service.OrderService
import com.rarible.protocol.nftorder.core.service.OwnershipService
import com.rarible.protocol.nftorder.listener.test.data.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OwnershipEventServiceTest {

    private val ownershipService: OwnershipService = mockk()
    private val itemEventService: ItemEventService = mockk()
    private val eventListener: OwnershipEventListener = mockk()
    private val orderService: OrderService = mockk()
    private val ownershipEventListeners = listOf(eventListener)
    private val bestOrderService: BestOrderService = mockk()

    private val ownershipEventService = OwnershipEventService(
        ownershipService,
        orderService,
        itemEventService,
        ownershipEventListeners,
        bestOrderService
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            ownershipService,
            itemEventService,
            eventListener,
            bestOrderService
        )
        coEvery { eventListener.onEvent(any()) } returns Unit
        coEvery { itemEventService.onOwnershipUpdated(any(), any()) } returns Unit
    }

    @Test
    fun `on ownership best sell order updated - fetched, order updated`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val ownership = randomOwnership(itemId)
        val nftOwnership = randomNftOwnershipDto(ownership.id)
        val order = randomLegacyOrderDto(itemId, ownership.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedOwnership = ownership.copy(bestSellOrder = shortOrder)

        // Ownership not fetched and enriched by received Order
        coEvery { ownershipService.getOrFetchOwnershipById(ownership.id) } returns Fetched(ownership, nftOwnership)
        coEvery { bestOrderService.getBestSellOrder(ownership, order) } returns shortOrder
        coEvery { ownershipService.save(expectedOwnership) } returns expectedOwnership
        coEvery { orderService.fetchOrderIfDiffers(shortOrder, order) } returns order

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order)

        // Listener should be notified, Ownership - saved and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.save(expectedOwnership) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(ownership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(ownership.id) }
    }

    @Test
    fun `on ownership best sell order updated - fetched, order cancelled`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val ownership = randomOwnership(itemId)
        val nftOwnership = randomNftOwnershipDto(ownership.id)
        val order = randomLegacyOrderDto(itemId, ownership.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedOwnership = ownership.copy(bestSellOrder = shortOrder)

        // Ownership fetched, best Order is cancelled - nothing should happen here
        coEvery { ownershipService.getOrFetchOwnershipById(ownership.id) } returns Fetched(ownership, nftOwnership)
        coEvery { bestOrderService.getBestSellOrder(ownership, order) } returns null

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order)

        // Since Ownership wasn't in DB and received Order is cancelled, we should just skip such update
        coVerify(exactly = 0) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(expectedOwnership) }
        coVerify(exactly = 0) { itemEventService.onOwnershipUpdated(ownership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(ownership.id) }
    }

    @Test
    fun `on ownership best sell order updated - not fetched, order updated`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val currentOrder = ShortOrderConverter.convert(randomLegacyOrderDto())
        val ownership = randomOwnership(itemId).copy(bestSellOrder = currentOrder)
        val order = randomLegacyOrderDto(itemId, ownership.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedOwnership = ownership.copy(bestSellOrder = shortOrder)

        // Ownership not fetched, current Order should be replaced by updated Order
        coEvery { ownershipService.getOrFetchOwnershipById(ownership.id) } returns Fetched(ownership, null)
        coEvery { bestOrderService.getBestSellOrder(ownership, order) } returns shortOrder
        coEvery { ownershipService.save(expectedOwnership) } returns expectedOwnership
        coEvery { orderService.fetchOrderIfDiffers(shortOrder, order) } returns order

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order)

        // Listener should be notified, Ownership - saved and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.save(expectedOwnership) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(ownership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(ownership.id) }
    }

    @Test
    fun `on ownership best sell order updated - not fetched, order cancelled`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val currentShortOrder = ShortOrderConverter.convert(randomLegacyOrderDto())
        val ownership = randomOwnership(itemId).copy(bestSellOrder = currentShortOrder)
        val order = randomLegacyOrderDto(itemId, ownership.id.owner).copy(cancelled = true)
        val shortOrder = ShortOrderConverter.convert(order)

        val expectedOwnership = ownership.copy(bestSellOrder = shortOrder)

        // Ownership not fetched, best Order is cancelled - Ownership should be deleted
        coEvery { ownershipService.getOrFetchOwnershipById(ownership.id) } returns Fetched(ownership, null)
        // Means order is cancelled
        coEvery { bestOrderService.getBestSellOrder(ownership, order) } returns null
        coEvery { ownershipService.delete(ownership.id) } returns DeleteResult.acknowledged(1)
        coEvery { orderService.fetchOrderIfDiffers(null, order) } returns null

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order)

        // Listener should be notified, Ownership - deleted and Item data should be recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(expectedOwnership) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(ownership.id, order) }
        coVerify(exactly = 1) { ownershipService.delete(ownership.id) }
    }

    @Test
    fun `on ownership best sell order updated - not fetched, order is the same`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val temp = randomOwnership(itemId)
        val order = randomLegacyOrderDto(itemId, temp.id.owner)
        val shortOrder = ShortOrderConverter.convert(order)
        val ownership = temp.copy(bestSellOrder = shortOrder)

        // Ownership not fetched, best Order is the same - nothing should happen here
        coEvery { ownershipService.getOrFetchOwnershipById(ownership.id) } returns Fetched(ownership, null)
        coEvery { bestOrderService.getBestSellOrder(ownership, order) } returns shortOrder

        ownershipEventService.onOwnershipBestSellOrderUpdated(ownership.id, order)

        // Since nothing changed for Ownership, and it's order, we should skip such update
        coVerify(exactly = 0) { eventListener.onEvent(any()) }
        coVerify(exactly = 0) { ownershipService.save(any()) }
        coVerify(exactly = 0) { itemEventService.onOwnershipUpdated(ownership.id, order) }
        coVerify(exactly = 0) { ownershipService.delete(ownership.id) }
    }

    @Test
    fun `on ownership deleted - success`() = runBlocking {
        val ownershipId = randomOwnershipId()
        val event = randomOwnershipDeleteEvent(ownershipId)


        coEvery { ownershipService.delete(ownershipId) } returns DeleteResult.acknowledged(1)

        ownershipEventService.onOwnershipDeleted(event.ownership)

        // Ownership deleted, listeners notified, item recalculated
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.delete(ownershipId) }
        coVerify(exactly = 1) { itemEventService.onOwnershipUpdated(ownershipId, null) }
    }

    @Test
    fun `on ownership deleted - nothing to delete`() = runBlocking {
        val ownershipId = randomOwnershipId()
        val event = randomOwnershipDeleteEvent(ownershipId)


        coEvery { ownershipService.delete(ownershipId) } returns DeleteResult.acknowledged(0)

        ownershipEventService.onOwnershipDeleted(event.ownership)

        // Even we don't have Ownership in DB, we need to notify listeners, but we should not recalculate Item sell stat
        coVerify(exactly = 1) { eventListener.onEvent(any()) }
        coVerify(exactly = 1) { ownershipService.delete(ownershipId) }
        coVerify(exactly = 0) { itemEventService.onOwnershipUpdated(ownershipId, null) }
    }

}