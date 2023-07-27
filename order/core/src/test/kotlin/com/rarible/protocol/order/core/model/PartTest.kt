package com.rarible.protocol.order.core.model

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.ethereum.domain.EthUInt256
import io.daonomic.rpc.domain.Binary
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.util.stream.Stream
import kotlin.math.absoluteValue

internal class PartTest {
    companion object {
        @JvmStatic
        fun parts(): Stream<Part> {
            return Stream.of(
                Part(randomAddress(), EthUInt256.of(randomInt().absoluteValue)),
                Part(Address.ZERO(), EthUInt256.ZERO),
                Part(Address.ONE(), EthUInt256.ZERO),
                Part(Address.ONE(), EthUInt256.ONE)
            )
        }
    }

    @ParameterizedTest
    @MethodSource("parts")
    fun `should decode part to BigInteger`(part: Part) {
        val integer = part.toBigInteger()
        val decodedPart = Part.from(integer)

        assertThat(decodedPart.account).isEqualTo(part.account)
        assertThat(decodedPart.value).isEqualTo(part.value)
    }

    @Test
    fun `should decode from BigInteger`() {
        val fixAccount = Address.apply("0x3a9acb0c510cedf6e74aca3c3b6dd56b8c706895")

        run {
            val integer = Binary.apply("0x0000000000000000000000003a9acb0c510cedf6e74aca3c3b6dd56b8c706895").toBigInteger()
            val decodedPart = Part.from(integer)

            assertThat(decodedPart.account).isEqualTo(fixAccount)
            assertThat(decodedPart.value).isEqualTo(EthUInt256.of(0))
        }
        run {
            val integer = Binary.apply("0x0000000000000000000000643a9acb0c510cedf6e74aca3c3b6dd56b8c706895").toBigInteger()
            val decodedPart = Part.from(integer)

            assertThat(decodedPart.account).isEqualTo(fixAccount)
            assertThat(decodedPart.value).isEqualTo(EthUInt256.of(100))
        }
        run {
            val integer = Binary.apply("0x0000000000000000000013883a9acb0c510cedf6e74aca3c3b6dd56b8c706895").toBigInteger()
            val decodedPart = Part.from(integer)

            assertThat(decodedPart.account).isEqualTo(fixAccount)
            assertThat(decodedPart.value).isEqualTo(EthUInt256.of(5_000))
        }
        run {
            val integer = Binary.apply("0x0000000000000000000027103a9acb0c510cedf6e74aca3c3b6dd56b8c706895").toBigInteger()
            val decodedPart = Part.from(integer)

            assertThat(decodedPart.account).isEqualTo(fixAccount)
            assertThat(decodedPart.value).isEqualTo(EthUInt256.of(10_000))
        }
    }
}
