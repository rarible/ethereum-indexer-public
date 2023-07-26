package com.rarible.protocol.order.core.misc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import java.util.stream.Stream

internal class BinaryExtensionKtTest {
    private companion object {
        @JvmStatic
        fun hexTestCases(): Stream<Arguments> = Stream.of(
            Arguments.of("0x", "0x", null),
            Arguments.of("0xf", "0x0f", BigInteger.valueOf(15)),
            Arguments.of("0x12345", "0x012345", BigInteger.valueOf(74565)),
            Arguments.of("0xabff", "0xabff", BigInteger.valueOf(44031)),
        )
    }

    @ParameterizedTest
    @MethodSource("hexTestCases")
    fun `should fix hex`(wrong: String, expectedHex: String, expectedBigInteger: BigInteger?) {
        assertThat(wrong.paddingHex()).isEqualTo(expectedHex)
        assertThat(wrong.fromHexToBigInteger()).isEqualTo(expectedBigInteger)
    }
}
