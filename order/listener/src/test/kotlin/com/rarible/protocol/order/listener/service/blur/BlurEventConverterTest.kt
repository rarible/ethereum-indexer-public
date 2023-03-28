package com.rarible.protocol.order.listener.service.blur

import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.log
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.TokenStandard
import com.rarible.protocol.order.core.parser.BlurOrderParser
import com.rarible.protocol.order.core.service.PriceNormalizer
import com.rarible.protocol.order.core.service.PriceUpdateService
import com.rarible.protocol.order.core.trace.TraceCallService
import com.rarible.protocol.order.listener.service.looksrare.TokenStandardProvider
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.response.Transaction
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

class BlurEventConverterTest {
    private val contractService = mockk<ContractService>()
    private val traceCallService = mockk<TraceCallService>()
    private val featureFlags = mockk<OrderIndexerProperties.FeatureFlags>()
    private val standardProvider = mockk<TokenStandardProvider>()
    private val priceUpdateService = mockk<PriceUpdateService>()
    private val prizeNormalizer = PriceNormalizer(contractService)

    private val converter = BlurEventConverter(
        traceCallService,
        featureFlags,
        standardProvider,
        priceUpdateService,
        prizeNormalizer
    )

    @Test
    fun `convert cancel`() = runBlocking<Unit> {
        val collection = Address.apply("0x394E3d3044fC89fCDd966D3cb35Ac0B32B0Cda91")
        val tokenId = EthUInt256.of(7774)
        val log = log(
            topics = listOf(
                Word.apply("0x5152abf959f6564662358c2e52b702259b78bac5ee7842a0f01937e670efcc7d"),
            ),
            data = "0xB9D227C83C244B1F4D05F322ACD8CF78062F1AD8FF4DE58A2639D3BBB2308572"
        )
        val transient = mockk<Transaction> {
            every { input() } returns cancelOrderTx
            every { hash() } returns Word.apply(randomWord())
        }
        coEvery {
            standardProvider.getTokenStandard(collection)
        } returns TokenStandard.ERC721

        val expectedDate = Instant.now()
        val expectedMake = Asset(Erc721AssetType(collection, tokenId), EthUInt256.ONE)
        val expectedTake = Asset(EthAssetType, EthUInt256.of(BigInteger("1370000000000000000")))

        val cancels = converter.convertToCancel(log, transient, 0, 1, expectedDate)
        assertThat(cancels).hasSize(1)
        assertThat(cancels.single().hash).isEqualTo(Word.apply("0xB9D227C83C244B1F4D05F322ACD8CF78062F1AD8FF4DE58A2639D3BBB2308572"))
        assertThat(cancels.single().source).isEqualTo(HistorySource.BLUR)
        assertThat(cancels.single().date).isEqualTo(expectedDate)
        assertThat(cancels.single().make).isEqualTo(expectedMake)
        assertThat(cancels.single().take).isEqualTo(expectedTake)
        assertThat(cancels.single().maker).isEqualTo(Address.apply("0x1ab608B76dE67507E1d70441Ee9282FEFa17a334"))
    }

    @Test
    fun `convert nonce change`() = runBlocking<Unit> {
        //https://etherscan.io/tx/0xcc232528b5bc28774de6a082dbe2f629689fd8e1c51b790d3a6fafc6eafd3020#eventlog
        val log = log(
            topics = listOf(
                Word.apply("0xa82a649bbd060c9099cd7b7326e2b0dc9e9af0836480e0f849dc9eaa79710b3b"),
                Word.apply("0x000000000000000000000000ed2ab4948ba6a909a7751dec4f34f303eb8c7236"),
            ),
            data = "0x0000000000000000000000000000000000000000000000000000000000000045"
        )
        val expectedDate = Instant.now()
        val nonceChanges = converter.convertChangeNonce(log, expectedDate)
        assertThat(nonceChanges).hasSize(1)
        assertThat(nonceChanges.single().maker).isEqualTo(Address.apply("0xed2ab4948bA6A909a7751DEc4F34f303eB8c7236"))
        assertThat(nonceChanges.single().newNonce).isEqualTo(EthUInt256.of(69))
        assertThat(nonceChanges.single().date).isEqualTo(expectedDate)
        assertThat(nonceChanges.single().source).isEqualTo(HistorySource.BLUR)
    }

    @Test
    fun `convert match`() = runBlocking<Unit> {
        val date = Instant.now()
        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val collection = Address.apply("0x60e4d786628fea6478f785a6d7e704777c86a7c6")
        val tokenId = EthUInt256.of(13414)
        val log = log(
            topics = listOf(
                Word.apply("0x61cbb2a3dee0b6064c2e681aadd61677fb4ef319f0b547508d495626f5a62f64"),
                Word.apply("0x0000000000000000000000003d606dd3a1b8e2519f9f955439e52184d4c80fa3"),
                Word.apply("0x0000000000000000000000004aee91e7d6c6e0c4c0ec10b9757b8d19ab7f427c"),
            ),
            data = matchedOrdersEventData.prefixed()
        )
        val transient = mockk<Transaction> {
            every { input() } returns matchOrderTxData
            every { hash() } returns Word.apply(randomWord())
        }
        coEvery {
            standardProvider.getTokenStandard(collection)
        } returns TokenStandard.ERC721

        val expectedDate = Instant.now()
        val expectedMake = Asset(Erc721AssetType(collection, tokenId), EthUInt256.ONE)
        val expectedTake = Asset(EthAssetType, EthUInt256.of(BigInteger("1370000000000000000")))

        val expectedNft = Asset(
            Erc721AssetType(collection, tokenId),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("13969800000000000000")
        )

        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedNft, take = expectedPayment, at = date) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = expectedPayment, take = expectedNft, at = date) } returns bidOrderUsd

        val matches = converter.convertToSideMatch(log, transient, 0, 1, date)

        assertThat(matches).hasSize(2)
        val left = matches[0]
        assertThat(left.hash).isEqualTo(Word.apply("0xec7ddc6fdbc5bf498dfee48f89f54e07f65d009650dfdbe8e63bdaca86f68529"))
        assertThat(left.side).isEqualTo(OrderSide.LEFT)
        assertThat(left.maker).isEqualTo(Address.apply("0x3d606dd3a1b8e2519f9f955439e52184d4c80fa3"))
        assertThat(left.taker).isEqualTo(Address.apply("0x4aee91e7d6c6e0c4c0ec10b9757b8d19ab7f427c"))
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedPayment.value)
        assertThat(left.date).isEqualTo(date)
        assertThat(left.makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(left.takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(left.makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(left.takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)
        assertThat(left.takeValue).isEqualTo(BigDecimal("13.969800000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.BLUR)
        assertThat(left.adhoc).isFalse
        assertThat(left.counterAdhoc).isTrue

        val right = matches[1]
        assertThat(right.counterHash).isEqualTo(Word.apply("0xec7ddc6fdbc5bf498dfee48f89f54e07f65d009650dfdbe8e63bdaca86f68529"))
        assertThat(right.side).isEqualTo(OrderSide.RIGHT)
        assertThat(right.maker).isEqualTo(Address.apply("0x4aee91e7d6c6e0c4c0ec10b9757b8d19ab7f427c"))
        assertThat(right.taker).isEqualTo(Address.apply("0x3d606dd3a1b8e2519f9f955439e52184d4c80fa3"))
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.date).isEqualTo(date)
        assertThat(right.makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(right.takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(right.makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(right.takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
        assertThat(right.makeValue).isEqualTo(BigDecimal("13.969800000000000000"))
        assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.source).isEqualTo(HistorySource.BLUR)
        assertThat(left.adhoc).isFalse
        assertThat(left.counterAdhoc).isTrue()
    }

    @Test
    fun `parse - ok, batchBuyWithERC20s=`() {
        val result = BlurOrderParser.parseTradeDetails(batchBuyWithERC20, Word.apply(randomWord()))
        assertThat(result.single().marketId).isEqualTo(BigInteger("7"))
        assertThat(result.single().value).isEqualTo(BigInteger("234900000000000000"))
        assertThat(result.single().tradeData.hex()).startsWith("f7fc708b")
    }

    @Test
    fun `parse - ok, try find execution`() {
        val result = BlurOrderParser.parseTradeDetails(batchBuyWithERC20, Word.apply(randomWord()))
        val executions = BlurOrderParser.tryFetchExecutions(result.map { it.tradeData }, Word.apply(randomWord()))

        assertThat(executions.single())
            .isEqualTo(
                BlurOrderParser.parseExecutions(executionFromTrace, Word.apply(randomWord())).single()
            )
    }

    //https://etherscan.io/tx/0xc10e943c243ff056278991efbd5221d4042a7515f2ef06cab25dfce03acc1a4b#eventlog
    private val matchOrderTxData = Binary.apply(
        "0x9a1fc3a7000000000000000000000000000000000000000000000000000000" +
                "0000000040000000000000000000000000000000000000000000000000000" +
                "00000000004800000000000000000000000000000000000000000000000000" +
                "0000000000000e000000000000000000000000000000000000000000000000" +
                "0000000000000001be7a8b1a4b95509caecf77d340a28e09b860b77b1a45f6" +
                "142dea47cad752c51ff18c4079a556928bae43f922406491c08132eb96c784" +
                "9b9303d8b19fae8511bc200000000000000000000000000000000000000000" +
                "00000000000000000000320000000000000000000000000000000000000000" +
                "00000000000000000000000010000000000000000000000000000000000000" +
                "000000000000000000000fbb5030000000000000000000000003d606dd3a1b" +
                "8e2519f9f955439e52184d4c80fa3000000000000000000000000000000000" +
                "00000000000000000000000000000010000000000000000000000000000000" +
                "000dab4a563819e8fd93dba3b25bc349500000000000000000000000060e4d" +
                "786628fea6478f785a6d7e704777c86a7c6000000000000000000000000000" +
                "00000000000000000000000000000000034660000000000000000000000000" +
                "00000000000000000000000000000000000000100000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000c1deb315b74080000000000000000000000" +
                "000000000000000000000000000000000000063d3576d00000000000000000" +
                "00000000000000000000000000000000000000063dc91ed000000000000000" +
                "00000000000000000000000000000000000000000000001a00000000000000" +
                "000000000000000000008d88f2b78d2be0e687ac7ac6d60edc100000000000" +
                "00000000000000000000000000000000000000000000000000200000000000" +
                "00000000000000000000000000000000000000000000000000000010000000" +
                "00000000000000000000000000000000000000000000000000000003200000" +
                "0000000000000000000a858ddc0445d8131dac4d1de01f834ffcba52ef1000" +
                "00000000000000000000000000000000000000000000000000000000000010" +
                "10000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "10000000000000000000000000000000000000000000000000000000000000" +
                "00080000000000000000000000000000000000000000000000000000000000" +
                "000001be61d08d0a351eec5cbd6121633d89c17b7c6a75594875ef1cd8f080" +
                "d0ce665c73b4becd5594f78791f12e14b9396b41719dbbb058dce9f1105fbc" +
                "1674d6b55a3000000000000000000000000000000000000000000000000000" +
                "0000000000003be71759cad8c3f62933f749b4c3303b5b3021722c15587a3c" +
                "66d0d710caf1128f9785f168b87be4db00de6ab36305c3fa4327a84d502264" +
                "8cd76497196ed96d5f64daede2a122378484fef80c6702f225a50a30f9b57a" +
                "7aa02a7e7120bd8b37c0000000000000000000000000000000000000000000" +
                "0000000000000000000e000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000002e0000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000fbb5030000000000000000000000004aee9" +
                "1e7d6c6e0c4c0ec10b9757b8d19ab7f427c000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000dab4a563819e8fd93dba3b25bc349500000000000000000000000" +
                "060e4d786628fea6478f785a6d7e704777c86a7c6000000000000000000000" +
                "00000000000000000000000000000000000000034660000000000000000000" +
                "00000000000000000000000000000000000000000000100000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000c1deb315b74080000000000000000" +
                "000000000000000000000000000000000000000000063d3576e00000000000" +
                "00000000000000000000000000000000000000000000063d365f8000000000" +
                "00000000000000000000000000000000000000000000000000001a00000000" +
                "0000000000000000000000000d87b7d4a408d7f682088aec27ae7dbad00000" +
                "000000000000000000000000000000000000000000000000000000001c0000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "10100000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000" +
                "00060000000000000000000000000000000000000000000000000000000000" +
                "000001c651610a7411c8d076c2d99bf1573ea4a07a860a9393fb74030001c2" +
                "78a8de5ef20e42f8a60a9de78be2a8592fef865a7483c38173c4c126b62673" +
                "82bea38dc10"
    )
    private val matchedOrdersEventData = Binary.apply(
        "0000000000000000000000000000000000000000000000000000000000000080" +
                "ec7ddc6fdbc5bf498dfee48f89f54e07f65d009650dfdbe8e63bdaca86f68529" +
                "00000000000000000000000000000000000000000000000000000000000002c0" +
                "592b3d6545002be4cf3843626d36de5064519c531c66130f5811f7f3c999aa1a" +
                "0000000000000000000000003d606dd3a1b8e2519f9f955439e52184d4c80fa3" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000dab4a563819e8fd93dba3b25bc3495" +
                "00000000000000000000000060e4d786628fea6478f785a6d7e704777c86a7c6" +
                "0000000000000000000000000000000000000000000000000000000000003466" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000c1deb315b7408000" +
                "0000000000000000000000000000000000000000000000000000000063d3576d" +
                "0000000000000000000000000000000000000000000000000000000063dc91ed" +
                "00000000000000000000000000000000000000000000000000000000000001a0" +
                "0000000000000000000000000000000008d88f2b78d2be0e687ac7ac6d60edc1" +
                "0000000000000000000000000000000000000000000000000000000000000200" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000032" +
                "000000000000000000000000a858ddc0445d8131dac4d1de01f834ffcba52ef1" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0100000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000004aee91e7d6c6e0c4c0ec10b9757b8d19ab7f427c" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000dab4a563819e8fd93dba3b25bc3495" +
                "00000000000000000000000060e4d786628fea6478f785a6d7e704777c86a7c6" +
                "0000000000000000000000000000000000000000000000000000000000003466" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000c1deb315b7408000" +
                "0000000000000000000000000000000000000000000000000000000063d3576e" +
                "0000000000000000000000000000000000000000000000000000000063d365f8" +
                "00000000000000000000000000000000000000000000000000000000000001a0" +
                "00000000000000000000000000000000d87b7d4a408d7f682088aec27ae7dbad" +
                "00000000000000000000000000000000000000000000000000000000000001c0" +
                "0000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000001" +
                "0100000000000000000000000000000000000000000000000000000000000000"
    )

    //https://etherscan.io/tx/0x3e9721a6c90445d3847a6792138f856cc14891f027af233f5b52c4c3463ceb22
    private val cancelOrderTx = Binary.apply(
        "0xf4acd74000000000000000000000000000000000000000000000000000000000000000200000000000000000000000001ab608b7" +
                "6de67507e1d70441ee9282fefa17a33400000000000000000000000000000000000000000000000000000000000000010000000" +
                "000000000000000000000000000dab4a563819e8fd93dba3b25bc3495000000000000000000000000394e3d3044fc89fcdd966d" +
                "3cb35ac0b32b0cda910000000000000000000000000000000000000000000000000000000000001e5e000000000000000000000" +
                "0000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000130337bdce49000000000000000000000000000000000000000" +
                "00000000000000000000063d1511c0000000000000000000000000000000000000000000000000000000063da8b9b0000000000" +
                "0000000000000000000000000000000000000000000000000001a000000000000000000000000000000000402d9c7808533880c" +
                "d3f9b2982b299790000000000000000000000000000000000000000000000000000000000000200000000000000000000000000" +
                "0000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000003" +
                "200000000000000000000000060d190772500faca6a32e2b88ff0cfe5d9d7514200000000000000000000000000000000000000" +
                "000000000000000000000000010100000000000000000000000000000000000000000000000000000000000000"
    )

    //https://etherscan.io/tx/0x19c65f2c9b2fbe7bdf4cf8863afb93a9ed864a40b15014549b2d9e2538a07e2b
    private val batchBuyWithERC20 = Binary.apply(
        "0x09ba153d000000000000000000000000000000000000000000000000000000000000008000000000000000000000000000000000" +
                "000000000000000000000000000001400000000000000000000000000000000000000000000000000000000000000b000000000" +
                "000000000000000000000000000000000000000000000000000000bc00000000000000000000000000000000000000000000000" +
                "0000000000000000400000000000000000000000000000000000000000000000000000000000000080000000000000000000000" +
                "00000000000000000000000000000000000000000010000000000000000000000000000000000a39bb272e79075ade125fd3518" +
                "87ac000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000" +
                "000000000000001d26057c7fb308f00000000000000000000000000000000000000000000000000000000000000010000000000" +
                "0000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000" +
                "0000000000000070000000000000000000000000000000000000000000000000342884e31bd4000000000000000000000000000" +
                "000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000008e" +
                "4f7fc708b0000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000" +
                "00000000000000000000000000000008a00000000000000000000000000000000000000000000000000342884e31bd400000000" +
                "0000000000000000000d774557b647330c91bf44cfeab205095f7e6c36700000000000000000000000000000000000000000000" +
                "00000000000001023aa7000000000000000000000000000000000000000000000000000000000000001b45de51ea4fb6a2d0333" +
                "91b832b01d37897f357512614b033e24cc9b08a254110455bbce210157e3e59ae136706368642b49799d54ae3f55f6a276a7fdd" +
                "57e32e00000000000000000000000000000000000000000000000000000000000007649a1fc3a70000000000000000000000000" +
                "0000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000400" +
                "00000000000000000000000000000000000000000000000000000000000000e0000000000000000000000000000000000000000" +
                "000000000000000000000001b20c1ac77d891b902b8df792428145d654057be2dbb3d7a76e851955991d07dc53bbc8bb8f1837f" +
                "e225644441bca1a34c0e2de1514892e22f192763e3f27ffc8a00000000000000000000000000000000000000000000000000000" +
                "000000002e000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000" +
                "000000000000000000000000000001023aa70000000000000000000000003e528ad9f3975f247243336d28596be4965b3c59000" +
                "00000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000dab4a563" +
                "819e8fd93dba3b25bc3495000000000000000000000000d774557b647330c91bf44cfeab205095f7e6c36700000000000000000" +
                "0000000000000000000000000000000000000000000365e00000000000000000000000000000000000000000000000000000000" +
                "0000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000342884e31bd40000000000000000000000000000000000000000000000000000000000064226166000000" +
                "0000000000000000000000000000000000000000000000000064226f77000000000000000000000000000000000000000000000" +
                "00000000000000001a0000000000000000000000000000000002d0310809ccb3e81b09c5a6c3c2f8d4d00000000000000000000" +
                "000000000000000000000000000000000000000001c000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000010100000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c0000000000" +
                "0000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000" +
                "000000000000001b34c74c645cb890bb52d442d51373b1c0ce07b92629838a4fe0202a2100b02d010fed24bdaffbd58c932bcac" +
                "2536d94ad95c9332eca3daa910da9917164be118c00000000000000000000000000000000000000000000000000000000000000" +
                "0174f4ea3f93b85f5f324eb17715eabcabf32777773c3adfc85a540dc502529f230000000000000000000000000000000000000" +
                "0000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000002e000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001023aa70" +
                "0000000000000000000000039da41747a83aee658334415666f3ef92dd0d5410000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000dab4a563819e8fd93dba3b25bc3495000000000000000" +
                "000000000d774557b647330c91bf44cfeab205095f7e6c367000000000000000000000000000000000000000000000000000000" +
                "000000365e000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000342884e31bd40000000" +
                "0000000000000000000000000000000000000000000000000000642261670000000000000000000000000000000000000000000" +
                "00000000000006422732c00000000000000000000000000000000000000000000000000000000000001a0000000000000000000" +
                "00000000000000a8e634d102628ce38a973ce4d7f887fc000000000000000000000000000000000000000000000000000000000" +
                "00001c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000101000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000600000000000000000000000000000000000000000000000" +
                "00000000000000001b8fad88029568f78643a2ba744edd58631983cc591cd2eb9e0d1546fda21e5c7079c43fa395b56edbdf9d7" +
                "bd177383d499cbe9ee07c216e360f6fd484751b6cf2000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000" +
                "00000000000000000365e0000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000200" +
                "0000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000" +
                "0000000000000000000000242e1a7d4d00000000000000000000000000000000000000000000000001d26057c7fb308f0000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000"
    )
    private val executionFromTrace = Binary.apply(
        "0x9a1fc3a7000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000" +
                "0000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000000e00000000" +
                "00000000000000000000000000000000000000000000000000000001b20c1ac77d891b902b8df792428145d654057be2dbb3d7a" +
                "76e851955991d07dc53bbc8bb8f1837fe225644441bca1a34c0e2de1514892e22f192763e3f27ffc8a000000000000000000000" +
                "00000000000000000000000000000000000000002e0000000000000000000000000000000000000000000000000000000000000" +
                "00010000000000000000000000000000000000000000000000000000000001023aa70000000000000000000000003e528ad9f39" +
                "75f247243336d28596be4965b3c5900000000000000000000000000000000000000000000000000000000000000010000000000" +
                "000000000000000000000000dab4a563819e8fd93dba3b25bc3495000000000000000000000000d774557b647330c91bf44cfea" +
                "b205095f7e6c367000000000000000000000000000000000000000000000000000000000000365e000000000000000000000000" +
                "0000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000342884e31bd400000000000000000000000000000000000000000" +
                "000000000000000000642261660000000000000000000000000000000000000000000000000000000064226f770000000000000" +
                "0000000000000000000000000000000000000000000000001a0000000000000000000000000000000002d0310809ccb3e81b09c" +
                "5a6c3c2f8d4d00000000000000000000000000000000000000000000000000000000000001c0000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000101" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000c000000000000000000000000000000000000000000000000000000000000000800000000000000000" +
                "00000000000000000000000000000000000000000000001b34c74c645cb890bb52d442d51373b1c0ce07b92629838a4fe0202a2" +
                "100b02d010fed24bdaffbd58c932bcac2536d94ad95c9332eca3daa910da9917164be118c000000000000000000000000000000" +
                "000000000000000000000000000000000174f4ea3f93b85f5f324eb17715eabcabf32777773c3adfc85a540dc502529f2300000" +
                "000000000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0002e00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000001023aa700000000000000000000000039da41747a83aee658334415666f3ef92dd0d54100000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000dab4a563819e8" +
                "fd93dba3b25bc3495000000000000000000000000d774557b647330c91bf44cfeab205095f7e6c3670000000000000000000000" +
                "00000000000000000000000000000000000000365e0000000000000000000000000000000000000000000000000000000000000" +
                "0010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000342884e31bd4000000000000000000000000000000000000000000000000000000000006422616700000000000" +
                "0000000000000000000000000000000000000000000006422732c00000000000000000000000000000000000000000000000000" +
                "000000000001a000000000000000000000000000000000a8e634d102628ce38a973ce4d7f887fc0000000000000000000000000" +
                "0000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000001010000000000000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000" +
                "0000000000000000000000000000000000000000000000001b8fad88029568f78643a2ba744edd58631983cc591cd2eb9e0d1546" +
                "fda21e5c7079c43fa395b56edbdf9d7bd177383d499cbe9ee07c216e360f6fd484751b6cf2"
    )
}