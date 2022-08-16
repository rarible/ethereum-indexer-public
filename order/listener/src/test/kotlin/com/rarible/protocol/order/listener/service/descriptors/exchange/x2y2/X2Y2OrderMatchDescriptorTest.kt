package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.telemetry.metrics.RegisteredCounter
import com.rarible.ethereum.common.keccak256
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.math.BigInteger
import java.util.stream.Stream
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

class X2Y2OrderMatchDescriptorTest {

    private val matchMetric = mockk<RegisteredCounter> { every { increment() } just Runs }

    private val converter = X2Y2EventConverter(
        mockk(),
        mockk {
              coEvery { getAssetsUsdValue(any(), any(), any()) } returns null
        },
        PriceNormalizer(mockk())
    )

    private val descriptor = X2Y2SellOrderMatchDescriptor(
        mockk { every { x2y2V1 } returns Address.ZERO() },
        converter, matchMetric
    )

    @ParameterizedTest
    @MethodSource("params")
    internal fun `should process match event`(
        log: Log,
        expectedHash: Word,
        expectedToken: Address,
        expectedTokenId: BigInteger,
        expectedSeller: Address,
        expectedBuyer: Address,
    ) {
        runBlocking {
            val expectedCounterHash = keccak256(expectedHash)
            val transaction = mockk<Transaction> { every { input() } returns Binary.empty() }
            val actual = descriptor.convert(log, transaction, 1L, 1, 1).toFlux().collectList().awaitSingle()
            assertThat(actual).isNotEmpty
            assertThat(actual.size).isEqualTo(2)

            val left = actual.first()
            assertThat(left.side).isEqualTo(OrderSide.LEFT)
            assertThat(left.hash).isEqualTo(expectedHash)
            assertThat(left.counterHash).isEqualTo(expectedCounterHash)
            assertThat(left.fill).isEqualTo(left.make.value)
            assertThat(left.maker).isEqualTo(expectedSeller)
            assertThat(left.taker).isEqualTo(expectedBuyer)
            assertThat(left.make.type).isExactlyInstanceOf(Erc721AssetType::class.java)
            assertThat((left.make.type as Erc721AssetType).token).isEqualTo(expectedToken)
            assertThat((left.make.type as Erc721AssetType).tokenId.value).isEqualTo(expectedTokenId)
            assertThat(left.source).isEqualTo(HistorySource.X2Y2)
            assertThat(left.adhoc).isTrue
            assertThat(left.counterAdhoc).isFalse

            val right = actual.last()
            assertThat(right.side).isEqualTo(OrderSide.RIGHT)
            assertThat(right.hash).isEqualTo(expectedCounterHash)
            assertThat(right.counterHash).isEqualTo(expectedHash)
            assertThat(right.fill).isEqualTo(right.make.value)
            assertThat(right.maker).isEqualTo(expectedBuyer)
            assertThat(right.taker).isEqualTo(expectedSeller)
            assertThat(right.make.type).isExactlyInstanceOf(EthAssetType::class.java)
            assertThat(right.make.value).isEqualTo(right.fill)
            assertThat(right.source).isEqualTo(HistorySource.X2Y2)
            assertThat(right.adhoc).isFalse
            assertThat(right.counterAdhoc).isTrue
            assertThat((right.take.type as Erc721AssetType).token).isEqualTo(expectedToken)
            assertThat((right.take.type as Erc721AssetType).tokenId.value).isEqualTo(expectedTokenId)

        }
    }

    companion object {

        @JvmStatic
        fun params(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    firstLog(),
                    Word.apply("0x335224567e1d71cd2cd6fb92fbf6621a783ba5e43a663dc622e53f489a4af7bb"),
                    Address.apply("0x22C1f6050E56d2876009903609a2cC3fEf83B415"),
                    BigInteger.valueOf(14692L),
                    Address.apply("0xaa1cc5543c558524c3db21d219fcee58af054f2c"),
                    Address.apply("0xb6103b35c1dfcfbc66aa6aa59e5dec376b79dd6a")
                ),
                Arguments.of(
                    secondLog(),
                    Word.apply("0x50af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832"),
                    Address.apply("0x86C35FA9665002C08801805280fF6a077B23c98A"),
                    BigInteger.valueOf(6271L),
                    Address.apply("0xb67b362b89b83fa1bea4545082968a0fcad7ce5f"),
                    Address.apply("0x6a81c10eb6829ee2378ed021920a516f75caa631")
                )
            )
        }

        // https://etherscan.io/tx/0xaa660644e4021d5f5435a13dd229ad3ee87ee792a91d30ed7a95a6f358561f7c
        private fun firstLog() = log(
            topics = listOf(
                Word.apply("0x3cbb63f144840e5b1b0a38a7c19211d2e89de4d7c5faf8b2d3c1776c302d1d33"),
                Word.apply("0x335224567e1d71cd2cd6fb92fbf6621a783ba5e43a663dc622e53f489a4af7bb")
            ),
            data = "000000000000000000000000aa1cc5543c558524c3db21d219fcee58af054f2c000000000000000000000000b6103b35c1dfcfbc66aa6aa59e5dec376b79dd6a00000000000000000000000000000000b9974470fb5a1c05c98ba28a6f33f84c00000000000000000000000000000000000000000000000000019a9fd5890b2e00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000006237c7df0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000354a6ba7a18000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000022c1f6050e56d2876009903609a2cc3fef83b415000000000000000000000000000000000000000000000000000000000000396400000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000354a6ba7a18000335224567e1d71cd2cd6fb92fbf6621a783ba5e43a663dc622e53f489a4af7bb000000000000000000000000f849de01b080adc3a814fabe1e2087475cf2e35400000000000000000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000180000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000004e20000000000000000000000000099ba539cde20ff8b87b18460024a9e3acc9e025"
        )

        // https://etherscan.io/tx/0x6d6bb34634c0771450aed8264c4f4e4140c7e66d1a782a3a78f925f08c14bda3
        private fun secondLog() = log(
            topics = listOf(
                Word.apply("0x3cbb63f144840e5b1b0a38a7c19211d2e89de4d7c5faf8b2d3c1776c302d1d33"),
                Word.apply("0x50af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832")
            ),
            data = "000000000000000000000000b67b362b89b83fa1bea4545082968a0fcad7ce5f0000000000000000000000006a81c10eb6829ee2378ed021920a516f75caa63100000000000000000000000000000000f0bdac92b0d6c40ff862c1470b00ea5100000000000000000000000000000000000000000000000000011042fff211ac00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000006210a75e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000007a1fe1602770000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000086c35fa9665002c08801805280ff6a077b23c98a000000000000000000000000000000000000000000000000000000000000187f00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000007a1fe160277000050af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832000000000000000000000000f849de01b080adc3a814fabe1e2087475cf2e35400000000000000000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000180000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000004e20000000000000000000000000099ba539cde20ff8b87b18460024a9e3acc9e025"

        )
    }
}
