package com.rarible.protocol.order.listener.service.descriptors.exchange.x2y2

import com.rarible.core.common.nowMillis
import com.rarible.core.contract.model.Erc20Token
import com.rarible.core.test.data.randomAddress
import com.rarible.ethereum.common.keccak256
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.service.ContractsProvider
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.listener.data.log
import com.rarible.protocol.order.core.metric.ForeignOrderMetrics
import com.rarible.protocol.order.listener.misc.convert
import com.rarible.protocol.order.listener.service.x2y2.X2Y2EventConverter
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import scalether.domain.response.Log
import java.math.BigInteger
import java.time.Instant
import java.util.stream.Stream

class X2Y2OrderMatchDescriptorTest {

    private val contractService = mockk<ContractService>()

    private val converter = X2Y2EventConverter(
        mockk(),
        mockk {
            coEvery { getAssetsUsdValue(any(), any(), any()) } returns null
        },
        PriceNormalizer(contractService), mockk()
    )
    private val contractsProvider = mockk<ContractsProvider>() {
        every { x2y2V1() } returns listOf(randomAddress())
    }
    private val metrics = mockk<ForeignOrderMetrics> {
        every { onOrderEventHandled(Platform.X2Y2, "match") } returns Unit
    }
    private val descriptor = X2Y2SellOrderMatchDescriptor(
        contractsProvider,
        converter,
        metrics
    )

    @ParameterizedTest
    @MethodSource("paramsSell")
    internal fun `should process sell match event`(
        log: Log,
        expectedHash: Word,
        expectedToken: Address,
        expectedTokenId: BigInteger,
        expectedSeller: Address,
        expectedBuyer: Address,
    ) {
        runBlocking {
            val expectedDate = Instant.ofEpochSecond(1)
            val expectedCounterHash = keccak256(expectedHash)

            val actual = descriptor.convert<OrderSideMatch>(log, expectedDate.epochSecond, 1, 1)

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
            assertThat(left.date).isEqualTo(expectedDate)
            assertThat(left.adhoc).isFalse
            assertThat(left.counterAdhoc).isTrue

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
            assertThat(right.adhoc).isTrue
            assertThat(right.counterAdhoc).isFalse
            assertThat(left.date).isEqualTo(expectedDate)
            assertThat((right.take.type as Erc721AssetType).token).isEqualTo(expectedToken)
            assertThat((right.take.type as Erc721AssetType).tokenId.value).isEqualTo(expectedTokenId)
        }
    }

    @ParameterizedTest
    @MethodSource("paramsBuy")
    internal fun `should process buy match event`(
        log: Log,
        expectedNftToken: Address,
        expectedNftTokenId: EthUInt256,
        expectedCurrency: Address,
        expectedSeller: Address,
        expectedBuyer: Address,
    ) {
        runBlocking {
            coEvery { contractService.get(expectedCurrency) } returns Erc20Token(decimals = 18, name = "1", symbol = "2", id = Address.ZERO())

            val actual = descriptor.convert<OrderSideMatch>(log, nowMillis().epochSecond, 1, 1)

            assertThat(actual).isNotEmpty
            assertThat(actual.size).isEqualTo(2)

            val left = actual.first()
            assertThat(left.maker).isEqualTo(expectedBuyer)
            assertThat(left.taker).isEqualTo(expectedSeller)
            assertThat(left.make.type).isEqualTo(Erc20AssetType(expectedCurrency))
            assertThat(left.take.type).isEqualTo(Erc721AssetType(expectedNftToken, expectedNftTokenId))

            val right = actual.last()
            assertThat(right.maker).isEqualTo(expectedSeller)
            assertThat(right.taker).isEqualTo(expectedBuyer)
            assertThat(right.make.type).isEqualTo(Erc721AssetType(expectedNftToken, expectedNftTokenId))
            assertThat(right.take.type).isEqualTo(Erc20AssetType(expectedCurrency))
        }
    }

    companion object {

        @JvmStatic
        fun paramsSell(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    firstSellLog(),
                    Word.apply("0x335224567e1d71cd2cd6fb92fbf6621a783ba5e43a663dc622e53f489a4af7bb"),
                    Address.apply("0x22C1f6050E56d2876009903609a2cC3fEf83B415"),
                    BigInteger.valueOf(14692L),
                    Address.apply("0xaa1cc5543c558524c3db21d219fcee58af054f2c"),
                    Address.apply("0xb6103b35c1dfcfbc66aa6aa59e5dec376b79dd6a")
                ),
                Arguments.of(
                    secondSellLog(),
                    Word.apply("0x50af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832"),
                    Address.apply("0x86C35FA9665002C08801805280fF6a077B23c98A"),
                    BigInteger.valueOf(6271L),
                    Address.apply("0xb67b362b89b83fa1bea4545082968a0fcad7ce5f"),
                    Address.apply("0x6a81c10eb6829ee2378ed021920a516f75caa631")
                )
            )
        }
        // https://etherscan.io/tx/0xaa660644e4021d5f5435a13dd229ad3ee87ee792a91d30ed7a95a6f358561f7c
        private fun firstSellLog() = log(
            topics = listOf(
                Word.apply("0x3cbb63f144840e5b1b0a38a7c19211d2e89de4d7c5faf8b2d3c1776c302d1d33"),
                Word.apply("0x335224567e1d71cd2cd6fb92fbf6621a783ba5e43a663dc622e53f489a4af7bb")
            ),
            data = "000000000000000000000000aa1cc5543c558524c3db21d219fcee58af054f2c000000000000000000000000b6103b35c1dfcfbc66aa6aa59e5dec376b79dd6a00000000000000000000000000000000b9974470fb5a1c05c98ba28a6f33f84c00000000000000000000000000000000000000000000000000019a9fd5890b2e00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000006237c7df0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000354a6ba7a18000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000022c1f6050e56d2876009903609a2cc3fef83b415000000000000000000000000000000000000000000000000000000000000396400000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000354a6ba7a18000335224567e1d71cd2cd6fb92fbf6621a783ba5e43a663dc622e53f489a4af7bb000000000000000000000000f849de01b080adc3a814fabe1e2087475cf2e35400000000000000000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000180000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000004e20000000000000000000000000099ba539cde20ff8b87b18460024a9e3acc9e025"
        )
        // https://etherscan.io/tx/0x6d6bb34634c0771450aed8264c4f4e4140c7e66d1a782a3a78f925f08c14bda3
        private fun secondSellLog() = log(
            topics = listOf(
                Word.apply("0x3cbb63f144840e5b1b0a38a7c19211d2e89de4d7c5faf8b2d3c1776c302d1d33"),
                Word.apply("0x50af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832")
            ),
            data = "000000000000000000000000b67b362b89b83fa1bea4545082968a0fcad7ce5f0000000000000000000000006a81c10eb6829ee2378ed021920a516f75caa63100000000000000000000000000000000f0bdac92b0d6c40ff862c1470b00ea5100000000000000000000000000000000000000000000000000011042fff211ac00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000006210a75e0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000001800000000000000000000000000000000000000000000000000000000000000260000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000007a1fe1602770000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000100000000000000000000000086c35fa9665002c08801805280ff6a077b23c98a000000000000000000000000000000000000000000000000000000000000187f00000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000007a1fe160277000050af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832000000000000000000000000f849de01b080adc3a814fabe1e2087475cf2e35400000000000000000000000000000000000000000000000000000000000001600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000180000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000004e20000000000000000000000000099ba539cde20ff8b87b18460024a9e3acc9e025"
        )

        @JvmStatic
        fun paramsBuy(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    bidLog(),
                    Address.apply("0x709d30f1f60f03d85a0ef33142ef3259392dc9e1"), // Token
                    EthUInt256.of("705"), // TokenId
                    Address.apply("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2"), // Currency
                    Address.apply("0xbfa30bbe5a9e5d252a92a8cc126691bba0e7a433"), // Seller
                    Address.apply("0xb2f4fd60e80b5eccbcb8c80e923c31b0aaab8e24"), // Buer
                ),
            )
        }

        // https://etherscan.io/tx/0xe85c6a8cbfba715d219495702f038dc6cedcd57313cc51126ee65bdc7603078b
        private fun bidLog() = log(
            topics = listOf(
                Word.apply("0x3cbb63f144840e5b1b0a38a7c19211d2e89de4d7c5faf8b2d3c1776c302d1d33"),
                Word.apply("0x50af3f3ef8e8c2b6a30e59232dd269cd550b188c360b99380feb09ab74957832")
            ),
            data = "000000000000000000000000b2f4fd60e80b5eccbcb8c80e923c31b0aaab8e24" +
                    "000000000000000000000000bfa30bbe5a9e5d252a92a8cc126691bba0e7a433" +
                    "00000000000000000000000000000000979605db8b47177f7951b1c5fe0c3b7f" +
                    "0000000000000000000000000000000000000000000000000002782efb93bf9a" +
                    "0000000000000000000000000000000000000000000000000000000000000003" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "000000000000000000000000000000000000000000000000000000006337d8ac" +
                    "000000000000000000000000c02aaa39b223fe8d0a0e5c4f27ead9083c756cc2" +
                    "0000000000000000000000000000000000000000000000000000000000000160" +
                    "0000000000000000000000000000000000000000000000000000000000000180" +
                    "0000000000000000000000000000000000000000000000000000000000000260" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "00000000000000000000000000000000000000000000000002c68af0bb140000" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000000080" +
                    "0000000000000000000000000000000000000000000000000000000000000020" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "000000000000000000000000709d30f1f60f03d85a0ef33142ef3259392dc9e1" +
                    "00000000000000000000000000000000000000000000000000000000000002c1" +
                    "0000000000000000000000000000000000000000000000000000000000000002" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "00000000000000000000000000000000000000000000000002c68af0bb140000" +
                    "564256d317d839a379a80a4e1616684f5a1eee34e9e27cbe438207965bcc484b" +
                    "000000000000000000000000f849de01b080adc3a814fabe1e2087475cf2e354" +
                    "0000000000000000000000000000000000000000000000000000000000000160" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000180" +
                    "0000000000000000000000000000000000000000000000000000000000000000" +
                    "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000001388" +
                    "000000000000000000000000d823c605807cc5e6bd6fc0d7e4eea50d3e2d66cd"
        )
    }
}
