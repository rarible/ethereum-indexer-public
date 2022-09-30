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
        val result = curve.getBuyInfo(
            curve = randomAddress(),
            spotPrice = BigInteger("3").eth(),
            delta = BigInteger("1").eth() / BigInteger.TEN,
            numItems = BigInteger("5"),
            feeMultiplier = BigInteger("5").eth()  / BigInteger.valueOf(1000),
            protocolFeeMultiplier = BigInteger("3").eth()  / BigInteger.valueOf(1000)
        )
        assertThat(result.newSpotPrice).isEqualTo(BigInteger("35").eth() / BigInteger.TEN)
        assertThat(result.newDelta).isEqualTo(BigInteger("1").eth() / BigInteger.TEN)
        assertThat(result.inputValue).isEqualTo(BigDecimal("16.632").eth())
        assertThat(result.protocolFee).isEqualTo(BigDecimal("0.0495").eth())
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
    fun getSellInfo() = runBlocking<Unit> {
        val result = curve.getSellInfo(
            curve = randomAddress(),
            spotPrice = BigInteger("3").eth(),
            delta = BigInteger("1").eth() / BigInteger.TEN,
            numItems = BigInteger("5"),
            feeMultiplier = BigInteger("5").eth()  / BigInteger.valueOf(1000),
            protocolFeeMultiplier = BigInteger("3").eth()  / BigInteger.valueOf(1000)
        )
        assertThat(result.newSpotPrice).isEqualTo(BigInteger("25").eth() / BigInteger.TEN)
        assertThat(result.newDelta).isEqualTo(BigInteger("1").eth() / BigInteger.TEN)
        assertThat(result.outputValue).isEqualTo(BigDecimal("13.888").eth())
        assertThat(result.protocolFee).isEqualTo(BigDecimal("0.042").eth())
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