package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.service.floor.FloorSellService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class MinimalPriceItemBidValidationTest {
    private val floorSellService = mockk<FloorSellService>()
    private val properties = OrderIndexerProperties.BidValidationProperties(
        minPriceUsd = BigDecimal("1"),
        minPercentFromFloorPrice = BigDecimal("0.75")
    )
    private val featureFlags = OrderIndexerProperties.FeatureFlags(checkMinimalBidPrice = true)
    private val validator = MinimalPriceItemBidValidation(floorSellService, featureFlags, properties)

    @Test
    fun `should validate ok bid takePriceUsd`() = runBlocking<Unit> {
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("100")
        )
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns BigDecimal("1")
        validator.validate(bid)
    }

    @Test
    fun `should validate ok bid takePriceUsd if it is equal to minimal from floor`() = runBlocking<Unit> {
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("75")
        )
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns BigDecimal("100")
        validator.validate(bid)
    }

    @Test
    fun `should validate ok bid takePriceUsd if it is equal to minimal price`() = runBlocking<Unit> {
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("1")
        )
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns null
        validator.validate(bid)
    }

    @Test
    fun `should validate ok bid takePriceUsd if it is grater when minimal price`() = runBlocking<Unit> {
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("2")
        )
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns null
        validator.validate(bid)
    }

    @Test
    fun `should throw error if takePriceUsd below minimal from floor`() = runBlocking<Unit> {
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("74")
        )
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns BigDecimal("100")
        assertThrows<OrderUpdateException> {
            validator.validate(bid)
        }
    }

    @Test
    fun `should throw error if takePriceUsd below minimal price`() = runBlocking<Unit> {
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("0.1")
        )
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns null
        assertThrows<OrderUpdateException> {
            validator.validate(bid)
        }
    }
}