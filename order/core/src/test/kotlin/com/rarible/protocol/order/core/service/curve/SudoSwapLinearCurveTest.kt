package com.rarible.protocol.order.core.service.curve

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.service.curve.PoolCurve.Companion.eth
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class SudoSwapLinearCurveTest {
    val curve = SudoSwapLinearCurve()

    @Test
    fun getBuyInfo() = runBlocking<Unit> {
        val spotPrice = BigInteger("3").eth()
        val delta = BigDecimal("0.1").eth()
        val numItems = BigInteger("5")
        val feeMultiplier = BigDecimal("0.005").eth()
        val protocolFeeMultiplier = BigDecimal("0.003").eth()

        val expectedInputValue = BigDecimal("16.632").eth()

        val result = curve.getBuyInfo(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        assertThat(result.newSpotPrice).isEqualTo(BigDecimal("3.5").eth())
        assertThat(result.newDelta).isEqualTo(BigDecimal("0.1").eth())
        assertThat(result.inputValue).isEqualTo(expectedInputValue)
        assertThat(result.protocolFee).isEqualTo(BigDecimal("0.0495").eth())

        val inputValue = curve.getBuyInputValues(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems.intValueExact(),
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        ).sumOf { it.value }

        assertThat(inputValue).isEqualTo(expectedInputValue)
    }

    @Test
    fun getBuyValues() = runBlocking<Unit> {
        val result = curve.getBuyInputValues(
            curve = randomAddress(),
            spotPrice = BigInteger("1").eth(),
            delta = BigDecimal("0.3").eth(),
            numItems = 3
        )
        assertThat(result).hasSize(3)
        assertThat(result[0].value).isEqualTo(BigDecimal("1.3").eth())
        assertThat(result[1].value).isEqualTo(BigDecimal("1.6").eth())
        assertThat(result[2].value).isEqualTo(BigDecimal("1.9").eth())
    }

    @Test
    fun getBuyValuesWithFees() = runBlocking<Unit> {
        val result = curve.getSellOutputValues(
            curve = randomAddress(),
            spotPrice = BigDecimal("0.5").eth(),
            delta = BigDecimal("0.1").eth(),
            feeMultiplier = BigDecimal("0.001").eth(),
            protocolFeeMultiplier = BigDecimal("0.005").eth(),
            numItems = 5
        )
        assertThat(result).hasSize(5)
        assertThat(result[0].value).isEqualTo(BigDecimal("0.497").eth())
        assertThat(result[1].value).isEqualTo(BigDecimal("0.3976").eth())
        assertThat(result[2].value).isEqualTo(BigDecimal("0.2982").eth())
        assertThat(result[3].value).isEqualTo(BigDecimal("0.1988").eth())
        assertThat(result[4].value).isEqualTo(BigDecimal("0.0994").eth())
    }

    @Test
    fun getSellInfo() = runBlocking<Unit> {
        val spotPrice = BigInteger("3").eth()
        val delta = BigDecimal("0.1").eth()
        val numItems = BigInteger("5")
        val feeMultiplier = BigDecimal("0.005").eth()
        val protocolFeeMultiplier = BigDecimal("0.003").eth()

        val expectedOutputValue = BigDecimal("13.888").eth()

        val result = curve.getSellInfo(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        assertThat(result.newSpotPrice).isEqualTo(BigInteger("25").eth() / BigInteger.TEN)
        assertThat(result.newDelta).isEqualTo(BigInteger("1").eth() / BigInteger.TEN)
        assertThat(result.outputValue).isEqualTo(expectedOutputValue)
        assertThat(result.protocolFee).isEqualTo(BigDecimal("0.042").eth())

        val outputValue = curve.getSellOutputValues(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems.intValueExact(),
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        ).sumOf { it.value }

        assertThat(outputValue).isEqualTo(expectedOutputValue)
    }

    @Test
    fun getSellValues() = runBlocking<Unit> {
        val result = curve.getSellOutputValues(
            curve = randomAddress(),
            spotPrice = BigInteger("1").eth(),
            delta = BigDecimal("0.3").eth(),
            numItems = 3
        )
        assertThat(result).hasSize(3)
        assertThat(result[0].value).isEqualTo(BigDecimal("1").eth())
        assertThat(result[1].value).isEqualTo(BigDecimal("0.7").eth())
        assertThat(result[2].value).isEqualTo(BigDecimal("0.4").eth())
    }
}
