package com.rarible.protocol.order.core.service.curve

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.service.curve.PoolCurve.Companion.eth
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class SudoSwapExponentialCurveTest {
    private val curve = SudoSwapExponentialCurve()

    @Test
    fun getBuyInfo() = runBlocking<Unit> {
        val spotPrice = BigInteger("3").eth()
        val delta = BigInteger("2").eth()
        val numItems = BigInteger("5")
        val feeMultiplier = BigDecimal("0.005").eth()
        val protocolFeeMultiplier = BigDecimal("0.003").eth()

        val expectedInputValue = BigDecimal("187.488").eth()

        val result = curve.getBuyInfo(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        Assertions.assertThat(result.newSpotPrice).isEqualTo(BigInteger("96").eth())
        Assertions.assertThat(result.newDelta).isEqualTo(BigInteger("2").eth())
        Assertions.assertThat(result.inputValue).isEqualTo(expectedInputValue)
        Assertions.assertThat(result.protocolFee).isEqualTo(BigDecimal("0.558").eth())

        val inputValue = curve.getBuyInputValues(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems.intValueExact(),
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        ).sumOf { it.value }

        Assertions.assertThat(inputValue).isEqualTo(expectedInputValue)
    }

    @Test
    fun getBuyValues() = runBlocking<Unit> {
        val result = curve.getBuyInputValues(
            curve = randomAddress(),
            spotPrice = BigInteger("1").eth(),
            delta = BigDecimal("2").eth(),
            numItems = 3
        )
        Assertions.assertThat(result).hasSize(3)
        Assertions.assertThat(result[0].value).isEqualTo(BigDecimal("2").eth())
        Assertions.assertThat(result[1].value).isEqualTo(BigDecimal("4").eth())
        Assertions.assertThat(result[2].value).isEqualTo(BigDecimal("8").eth())
    }

    @Test
    fun getSellInfo() = runBlocking<Unit> {
        val spotPrice = BigInteger("3").eth()
        val delta = BigInteger("2").eth()
        val numItems = BigInteger("5")
        val feeMultiplier = BigDecimal("0.005").eth()
        val protocolFeeMultiplier = BigDecimal("0.003").eth()

        val expectedOutputValue = BigDecimal("5.766").eth()

        val result = curve.getSellInfo(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems,
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        )
        Assertions.assertThat(result.newSpotPrice).isEqualTo(BigDecimal("0.09375").eth())
        Assertions.assertThat(result.newDelta).isEqualTo(BigInteger("2").eth())
        Assertions.assertThat(result.outputValue).isEqualTo(expectedOutputValue)
        Assertions.assertThat(result.protocolFee).isEqualTo(BigDecimal("0.0174375").eth())

        val inputValue = curve.getSellOutputValues(
            curve = randomAddress(),
            spotPrice = spotPrice,
            delta = delta,
            numItems = numItems.intValueExact(),
            feeMultiplier = feeMultiplier,
            protocolFeeMultiplier = protocolFeeMultiplier
        ).sumOf { it.value }

        Assertions.assertThat(inputValue).isEqualTo(expectedOutputValue)
    }

    @Test
    fun getSellValues() = runBlocking<Unit> {
        val result = curve.getSellOutputValues(
            curve = randomAddress(),
            spotPrice = BigInteger("8").eth(),
            delta = BigDecimal("2").eth(),
            numItems = 3
        )
        Assertions.assertThat(result).hasSize(3)
        Assertions.assertThat(result[0].value).isEqualTo(BigDecimal("8").eth())
        Assertions.assertThat(result[1].value).isEqualTo(BigDecimal("4").eth())
        Assertions.assertThat(result[2].value).isEqualTo(BigDecimal("2").eth())
    }
}
