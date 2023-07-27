package com.rarible.protocol.order.core.model

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

class AmountsTest {
    @Test
    fun notBid() {
        val results = calculateAmounts(2.toBigInteger(), BigInteger.TEN.pow(20), 1.toBigInteger(), false)
        Assertions.assertThat(results)
            .isEqualTo(Pair(BigInteger.TEN.pow(20).div(2.toBigInteger()), BigInteger.ONE))
    }

    @Test
    fun bid() {
        val results = calculateAmounts(BigInteger.TEN.pow(20), 2.toBigInteger(), 1.toBigInteger(), true)
        Assertions.assertThat(results)
            .isEqualTo(Pair(BigInteger.ONE, BigInteger.TEN.pow(20).div(2.toBigInteger())))
    }
}
