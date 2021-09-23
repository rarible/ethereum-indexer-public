package com.rarible.protocol.nftorder.listener.service

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.nftorder.listener.test.AbstractIntegrationTest
import com.rarible.protocol.nftorder.listener.test.IntegrationTest
import com.rarible.protocol.nftorder.listener.test.data.*
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger

@IntegrationTest
class BestOrderServiceIt : AbstractIntegrationTest() {

    @Autowired
    lateinit var bestOrderService: BestOrderService

    @Test
    fun `item best sell order - updated is alive, current null`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val item = randomItem(itemId).copy(bestSellOrder = null)
        val updated = randomLegacyOrderDto(itemId)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // If current best Order is missing, updated should be set as best Order
        assertThat(bestSellOrder).isEqualTo(updated)
    }

    @Test
    fun `item best sell order - updated is alive, current the same`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomLegacyOrderDto(itemId)
        val current = updated.copy(start = randomLong())
        val item = randomItem(itemId).copy(bestSellOrder = current)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // If current Order is the same as updated, updated should be set as best Order
        assertThat(bestSellOrder).isEqualTo(updated)
    }

    @Test
    fun `item best sell order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomLegacyOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        val current = randomLegacyOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE))
        val item = randomItem(itemId).copy(bestSellOrder = current)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // We have better sell Order, replacing current best Order
        assertThat(bestSellOrder).isEqualTo(updated)
    }

    @Test
    fun `item best sell order - updated is alive, current has preferred type`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomOpenSeaV1OrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        // Current has higher takePrice, but it has preferred type
        val current = randomLegacyOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE))
        val item = randomItem(itemId).copy(bestSellOrder = current)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(bestSellOrder).isEqualTo(current)
    }

    @Test
    fun `item best sell order - updated is alive, updated has preferred type`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomLegacyOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        // Current is better than updated, but updated has preferred type
        val current = randomOpenSeaV1OrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.minus(BigDecimal.ONE))
        val item = randomItem(itemId).copy(bestSellOrder = current)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(bestSellOrder).isEqualTo(updated)
    }

    @Test
    fun `item best sell order - updated is alive, both orders doesn't have preferred type`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomOpenSeaV1OrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        val current = randomOpenSeaV1OrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.plus(BigDecimal.ONE))
        val item = randomItem(itemId).copy(bestSellOrder = current)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Updated Order has better price, should be set as best Order
        assertThat(bestSellOrder).isEqualTo(updated)
    }

    @Test
    fun `item best sell order - updated is alive, current is still the best`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomLegacyOrderDto(itemId).copy(makePriceUsd = randomBigDecimal(3, 1))
        val current = randomLegacyOrderDto(itemId).copy(makePriceUsd = updated.makePriceUsd!!.minus(BigDecimal.ONE))
        val item = randomItem(itemId).copy(bestSellOrder = current)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        // Current best Order is still better than updated
        assertThat(bestSellOrder).isEqualTo(current)
    }

    @Test
    fun `item best sell order - updated is dead, current is null`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val item = randomItem(itemId).copy(bestSellOrder = null)
        val updated = randomLegacyOrderDto(itemId).copy(cancelled = true)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        assertThat(bestSellOrder).isNull()
    }

    @Test
    fun `item best sell order - updated is dead, current with preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val item = randomItem(itemId)
        val updated = randomLegacyOrderDto(itemId).copy(cancelled = true)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        assertThat(bestSellOrder).isEqualTo(item.bestSellOrder)
    }

    @Test
    fun `item best sell order - updated is dead, current without preferred type is not null`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val item = randomItem(itemId).copy(bestSellOrder = randomOpenSeaV1OrderDto(itemId))
        val updated = randomLegacyOrderDto(itemId).copy(makeStock = BigInteger.ZERO)

        val bestSellOrder = bestOrderService.getBestSellOrder(item, updated)

        assertThat(bestSellOrder).isEqualTo(item.bestSellOrder)
    }

    @Test
    fun `item best bid order - updated is alive, current is not the best anymore`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomLegacyOrderDto(itemId).copy(takePriceUsd = randomBigDecimal(3, 1))
        val current = randomLegacyOrderDto(itemId).copy(takePriceUsd = updated.takePriceUsd!!.minus(BigDecimal.ONE))
        val item = randomItem(itemId).copy(bestBidOrder = current)

        val bestBidOrder = bestOrderService.getBestBidOrder(item, updated)

        // We have better bid Order, replacing current best Order
        assertThat(bestBidOrder).isEqualTo(updated)
    }

    @Test
    fun `ownership best sell order - updated is dead, current is the same`() = runBlocking<Unit> {
        val itemId = randomItemId()
        val updated = randomLegacyOrderDto(itemId).copy(cancelled = true)
        val current = updated.copy(start = randomLong(), cancelled = false)
        val fetched = randomLegacyOrderDto(itemId)
        val ownership = randomOwnership(itemId).copy(bestSellOrder = current)
        val ownershipId = ownership.id

        orderControllerApiMock.mockGetSellOrdersByItem(ownershipId, fetched)

        val bestSellOrder = bestOrderService.getBestSellOrder(ownership, updated)

        // Dead best Order should be replaced by fetched Order
        assertThat(bestSellOrder).isEqualTo(fetched)
    }

}