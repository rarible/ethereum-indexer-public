package com.rarible.protocol.order.core.service.curve

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.order.core.service.curve.SudoSwapCurve.Companion.eth
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class SudoSwapExponentialCurveTest {
    private val curve = SudoSwapExponentialCurve()

    @Test
    fun getBuyInfo() = runBlocking<Unit> {
        val result = curve.getBuyInfo(
            curve = randomAddress(),
            spotPrice = BigInteger("3").eth(),
            delta = BigInteger("2").eth(),
            numItems = BigInteger("5"),
            feeMultiplier = BigInteger("5").eth()  / BigInteger.valueOf(1000),
            protocolFeeMultiplier = BigInteger("3").eth()  / BigInteger.valueOf(1000)
        )
        Assertions.assertThat(result.newSpotPrice).isEqualTo(BigInteger("96").eth())
        Assertions.assertThat(result.newDelta).isEqualTo(BigInteger("2").eth())
        Assertions.assertThat(result.inputValue).isEqualTo(BigDecimal("187.488").eth())
        Assertions.assertThat(result.protocolFee).isEqualTo(BigDecimal("0.558").eth())
    }

    @Test
    fun getSellInfo() = runBlocking<Unit> {
        val result = curve.getSellInfo(
            curve = randomAddress(),
            spotPrice = BigInteger("3").eth(),
            delta = BigInteger("2").eth(),
            numItems = BigInteger("5"),
            feeMultiplier = BigInteger("5").eth()  / BigInteger.valueOf(1000),
            protocolFeeMultiplier = BigInteger("3").eth()  / BigInteger.valueOf(1000)
        )
        Assertions.assertThat(result.newSpotPrice).isEqualTo(BigDecimal("0.09375").eth())
        Assertions.assertThat(result.newDelta).isEqualTo(BigInteger("2").eth())
        Assertions.assertThat(result.outputValue).isEqualTo(BigDecimal("5.766").eth())
        Assertions.assertThat(result.protocolFee).isEqualTo(BigDecimal("0.0174375").eth())
    }
}