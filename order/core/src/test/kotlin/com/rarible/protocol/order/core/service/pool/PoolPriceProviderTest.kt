package com.rarible.protocol.order.core.service.pool

import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createSellOrder
import com.rarible.protocol.order.core.data.createSudoSwapBuyInfo
import com.rarible.protocol.order.core.data.createSudoSwapSellInfo
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomEth
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.curve.PoolCurve
import com.rarible.protocol.order.core.service.curve.PoolCurve.Companion.eth
import com.rarible.protocol.order.core.service.sudoswap.SudoSwapProtocolFeeProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class PoolPriceProviderTest {
    private val sudoSwapCurve = mockk<PoolCurve>()
    private val sudoSwapProtocolFeeProvider = mockk<SudoSwapProtocolFeeProvider>()
    private val normalizer = PriceNormalizer(mockk())

    private val provider = PoolPriceProvider(sudoSwapCurve, sudoSwapProtocolFeeProvider, normalizer)

    @Test
    fun `should update sell amm order price`() = runBlocking<Unit> {
        val data = createOrderSudoSwapAmmDataV1()
        val order = createSellOrder().copy(type = OrderType.AMM, take = randomEth(), data = data)
        val protocolFeeMultiplier = randomBigInt()
        val expectedMakePrice = BigDecimal("0.500000000000000000")
        val sudoSwapBuyInfo = createSudoSwapBuyInfo().copy(inputValue = expectedMakePrice.eth())
        coEvery { sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory) } returns protocolFeeMultiplier
        coEvery {
            sudoSwapCurve.getBuyInfo(
                curve = data.bondingCurve,
                spotPrice = data.spotPrice,
                delta = data.delta,
                numItems = order.make.value.value,
                feeMultiplier = data.fee,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
        } returns sudoSwapBuyInfo
        val result = provider.updatePoolPrice(order)
        assertThat(result.make.value).isEqualTo(result.make.value)
        assertThat(result.makePrice).isEqualTo(expectedMakePrice)
        assertThat(result.take.value.value).isEqualTo(sudoSwapBuyInfo.inputValue)
    }

    @Test
    fun `should update bid amm order price`() = runBlocking<Unit> {
        val data = createOrderSudoSwapAmmDataV1()
        val order = randomOrder().copy(type = OrderType.AMM, make = randomEth(), take = randomErc721(), data = data)
        val protocolFeeMultiplier = randomBigInt()
        val expectedTakePrice = BigDecimal("0.500000000000000000")
        val sudoSwapSellInfo = createSudoSwapSellInfo().copy(outputValue = expectedTakePrice.eth())
        coEvery { sudoSwapProtocolFeeProvider.getProtocolFeeMultiplier(data.factory) } returns protocolFeeMultiplier
        coEvery {
            sudoSwapCurve.getSellInfo(
                curve = data.bondingCurve,
                spotPrice = data.spotPrice,
                delta = data.delta,
                numItems = order.take.value.value,
                feeMultiplier = data.fee,
                protocolFeeMultiplier = protocolFeeMultiplier
            )
        } returns sudoSwapSellInfo
        val result = provider.updatePoolPrice(order)
        assertThat(result.take.value).isEqualTo(result.take.value)
        assertThat(result.takePrice).isEqualTo(expectedTakePrice)
        assertThat(result.make.value.value).isEqualTo(sudoSwapSellInfo.outputValue)
    }
}
