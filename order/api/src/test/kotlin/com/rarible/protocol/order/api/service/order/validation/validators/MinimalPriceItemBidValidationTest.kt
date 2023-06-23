package com.rarible.protocol.order.api.service.order.validation.validators

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.data.randomCollection
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.exception.OrderUpdateException
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.service.floor.FloorSellService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.math.BigDecimal
import java.util.stream.Stream

internal class MinimalPriceItemBidValidationTest {
    private val floorSellService = mockk<FloorSellService>()
    private val properties = OrderIndexerProperties.BidValidationProperties(
        minPriceUsd = BigDecimal("1"),
        minPercentFromFloorPrice = BigDecimal("0.75")
    )
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val validator = MinimalPriceItemBidValidation(floorSellService, featureFlags, properties)

    @BeforeEach
    fun setupMockk() {
        every { featureFlags.checkMinimalBidPrice } returns true
        every { featureFlags.checkMinimalCollectionBidPriceOnly } returns false
    }

    private companion object {
        @JvmStatic
        fun bidOrder(): Stream<Arguments> {
            val token = randomAddress()
            return Stream.of(
                Arguments.of(
                    createOrderVersion().copy(
                        make = randomErc20(),
                        take = randomErc721(token),
                        takePriceUsd = BigDecimal("100")
                    ),
                    token
                ),
                Arguments.of(
                    createOrderVersion().copy(
                        make = randomErc20(),
                        take = randomCollection(token),
                        takePriceUsd = BigDecimal("100")
                    ),
                    token
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("bidOrder")
    fun `should validate ok bid takePriceUsd`(bid: OrderVersion, token: Address) = runBlocking<Unit> {
        coEvery { floorSellService.getFloorSellPriceUsd(token) } returns BigDecimal("1")
        validator.validate(bid)
        coVerify { floorSellService.getFloorSellPriceUsd(token) }
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

    @Test
    fun `should not validate for not collection bid`() = runBlocking<Unit> {
        every { featureFlags.checkMinimalCollectionBidPriceOnly } returns true
        val token = randomAddress()
        val bid = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(token),
            takePriceUsd = BigDecimal("100")
        )
        validator.validate(bid)
        coVerify(exactly = 0) { floorSellService.getFloorSellPriceUsd(token) }
    }
}