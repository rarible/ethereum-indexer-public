package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.randomBidOrderUsdValue
import com.rarible.protocol.order.core.data.randomSellOrderUsdValue
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.EthAssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.listener.data.log
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import scalether.abi.Uint32Type
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

class BlurV2Execution721PackedDescriptorTest : AbstractBlurV2ExecutionDescriptorTest() {
    private val descriptor = BlurV2Execution721PackedDescriptor(
        contractsProvider = contractsProvider,
        blurV2EventConverter = blurV2EventConverter,
        autoReduceService = autoReduceService,
    )

    @Test
    fun `convert single sell`() = runBlocking<Unit> {
        // https://etherscan.io/tx/0xc199fd412393246039e22dcd70a8231bad64acee868b4e08349f9ec6509284d5
        val log = log(
            topics = listOf(
                Word.apply("0x1d5e12b51dee5e4d34434576c3fb99714a85f57b0fd546ada4b0bddd736d12b2"),
            ),
            data = "0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa4000000000000000000020060083fa03a1747aafe4e0f0ec3880740f49d74f11580000000000354a6ba7a18000789e35a999c443fe6089544056f728239b8ffee7"
        )
        val transient = mockkTransaction(execution721PackedTx)
        val ethereumBlockchainLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transient,
            index = 0,
            total = 1
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x789e35a999c443fe6089544056f728239b8ffee7"),
                EthUInt256.of(8198)
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("015000000000000000")
        )

        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()

        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), at = any()) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), at = any()) } returns bidOrderUsd

        val matches = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog)
        assertThat(matches).hasSize(2)

        val left = matches.single { it.side == OrderSide.LEFT }
        assertThat(left.hash).isEqualTo(Word.apply("0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa40"))
        assertThat(left.maker).isEqualTo(Address.apply("0x83fa03a1747aafe4e0f0ec3880740f49d74f1158"))
        assertThat(left.taker).isEqualTo(Address.apply("0xf770e42fd25d745e9f25ab57cca3e96f7fe62d14"))
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(blockTimestamp)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.015000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.BLUR)
        assertThat(left.adhoc).isFalse
        assertThat(left.counterAdhoc).isTrue
        assertThat(left.originFees).isEmpty()

        val right = matches.single { it.side == OrderSide.RIGHT }
        assertThat(right.counterHash).isEqualTo(Word.apply("0x21b7d4ae06f010118c4c4e976182504160a6ad111f471ea8df38ad045ff7fa40"))
        assertThat(right.maker).isEqualTo(Address.apply("0xf770e42fd25d745e9f25ab57cca3e96f7fe62d14"))
        assertThat(right.taker).isEqualTo(Address.apply("0x83fa03a1747aafe4e0f0ec3880740f49d74f1158"))
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedPayment.value)
        assertThat(right.date).isEqualTo(blockTimestamp)
        assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.015000000000000000"))
        assertThat(right.source).isEqualTo(HistorySource.BLUR)
        assertThat(right.adhoc).isTrue
        assertThat(right.counterAdhoc).isFalse
        assertThat(right.originFees).isEmpty()
    }

    @Test
    fun `convert multi sell`() = runBlocking<Unit> {
        // https://etherscan.io/tx/0x372bdb0be920d0066251dbf9164ef0da5538a9083eac848ecfa60e2e3c12c87e#eventlog
        val log1 = log(
            topics = listOf(
                Word.apply("0x1d5e12b51dee5e4d34434576c3fb99714a85f57b0fd546ada4b0bddd736d12b2"),
            ),
            data = "0xe169f8af644ce38a96b0e965bec1a512e70f32df91c3389db44ba8ea353ad23100000000000000000004860056b8dbe783ce1945d7758ad82dabf9260b20692000000000003ee20e64864000789e35a999c443fe6089544056f728239b8ffee7"
        )
        val log2 = log(
            topics = listOf(
                Word.apply("0x1d5e12b51dee5e4d34434576c3fb99714a85f57b0fd546ada4b0bddd736d12b2"),
            ),
            data = "0x3bdf2102c79fcb71ea7e9960b22702005fa25b49e55f6349e4332e652f82def800000000000000000021c700b4d133c96d8833ea8a2275a1c0ce6d220466162400000000003ff2e795f50000789e35a999c443fe6089544056f728239b8ffee7"
        )
        val transient = mockkTransaction(multyExecution721PackedTx)
        val ethereumBlockchainLog1 = EthereumBlockchainLog(
            ethLog = log1,
            ethTransaction = transient,
            index = 0,
            total = 2
        )
        val ethereumBlockchainLog2 = EthereumBlockchainLog(
            ethLog = log2,
            ethTransaction = transient,
            index = 1,
            total = 2
        )
        every {
            sender.call(any())
        } returns Mono.just(Uint32Type.encode(BigInteger("0")))

        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()

        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), at = any()) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), at = any()) } returns bidOrderUsd

        val matches1 = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog1)

        assertThat(matches1).hasSize(2)
        assertThat(matches1.first().hash).isEqualTo(Word.apply("0xE169F8AF644CE38A96B0E965BEC1A512E70F32DF91C3389DB44BA8EA353AD231"))

        val matches2 = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog2)
        assertThat(matches2).hasSize(2)
        assertThat(matches2.first().hash).isEqualTo(Word.apply("0x3BDF2102C79FCB71EA7E9960B22702005FA25B49E55F6349E4332E652F82DEF8"))
    }

    @Test
    fun `checking usd prices`() = runBlocking<Unit> {
        // https://etherscan.io/tx/0x372bdb0be920d0066251dbf9164ef0da5538a9083eac848ecfa60e2e3c12c87e#eventlog
        val log1 = log(
            topics = listOf(
                Word.apply("0x1d5e12b51dee5e4d34434576c3fb99714a85f57b0fd546ada4b0bddd736d12b2"),
            ),
            data = "0xe169f8af644ce38a96b0e965bec1a512e70f32df91c3389db44ba8ea353ad23100000000000000000004860056b8dbe783ce1945d7758ad82dabf9260b20692000000000003ee20e64864000789e35a999c443fe6089544056f728239b8ffee7"
        )
        val transient = mockkTransaction(multyExecution721PackedTx)
        val ethereumBlockchainLog1 = EthereumBlockchainLog(
            ethLog = log1,
            ethTransaction = transient,
            index = 0,
            total = 2
        )
        every {
            sender.call(any())
        } returns Mono.just(Uint32Type.encode(BigInteger("0")))

        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()
        val nft = Asset(
            type = Erc721AssetType(
                token = Address.apply("0x789e35a999c443fe6089544056f728239b8ffee7"),
                tokenId = EthUInt256(1158.toBigInteger())
            ),
            value = EthUInt256.ONE
        )
        val payment = Asset(
            type = EthAssetType,
            value = EthUInt256(17700000000000000.toBigInteger())
        )
        coEvery { priceUpdateService.getAssetsUsdValue(make = nft, take = payment, at = any()) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(make = payment, take = nft, at = any()) } returns bidOrderUsd

        val matches1 = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog1)

        assertThat(matches1).hasSize(2)
        assertThat(matches1.first().hash).isEqualTo(Word.apply("0xE169F8AF644CE38A96B0E965BEC1A512E70F32DF91C3389DB44BA8EA353AD231"))

        assertThat(matches1.first().makeUsd).isEqualTo(sellOrderUsd.makeUsd)
        assertThat(matches1.first().takeUsd).isEqualTo(sellOrderUsd.takeUsd)
        assertThat(matches1.first().makePriceUsd).isEqualTo(sellOrderUsd.makePriceUsd)
        assertThat(matches1.first().takePriceUsd).isEqualTo(sellOrderUsd.takePriceUsd)

        assertThat(matches1.last().makeUsd).isEqualTo(bidOrderUsd.makeUsd)
        assertThat(matches1.last().takeUsd).isEqualTo(bidOrderUsd.takeUsd)
        assertThat(matches1.last().makePriceUsd).isEqualTo(bidOrderUsd.makePriceUsd)
        assertThat(matches1.last().takePriceUsd).isEqualTo(bidOrderUsd.takePriceUsd)
    }

    private val execution721PackedTx = Binary.apply(
        "0x70bce2d6000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000003a000000000000000000000000083fa03a1747aafe4e0f0ec3880740f49d74f1158000000000000000000000000789e35a999c443fe6089544056f728239b8ffee7fe5511c35e70a0393b16a2240106f5e64b2b05580511231fda3ca216b9690cb100000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000064c0c12600000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ea91ac90a6b98aec6a1cc324faff58b000000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002e0000000000000000000000000f770e42fd25d745e9f25ab57cca3e96f7fe62d140000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002006000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000354a6ba7a180000000000000000000000000000000000000000000000000000000000000002006000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000041c71899e4d8cbcc6d55b03ee381c781b8ad8f0edc0154eec793c396b0d13fd9ea122bcf8407fd688f2a89ce493cef4eb50ee87750557701f95e5fe349d959fde91b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000059662800ebc45c9ea81489b7885ff02c7dae30ed549d2c8ccf9405de945126e6473a1e8ee8795be93065c797cd87eb563aa27f74a98eda5243952f0bdc08b291821b010e786f6af68e5d010513ff70a3aaed9afeb8661116e6ce00000000000000332d1229"
    )

    private val multyExecution721PackedTx = Binary.apply(
        "0x3925c3c3000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000006e000000000000000000000000000000000000000000000000000000000000000c000000000000000000000000000000000000000000000000000000000000003200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005e0000000000000000000000000f770e42fd25d745e9f25ab57cca3e96f7fe62d14000000000000000000000000000000000000000000000000000000000000000200000000000000000000000056b8dbe783ce1945d7758ad82dabf9260b206920000000000000000000000000789e35a999c443fe6089544056f728239b8ffee7f84e5681c128b68e8723c8089f5acf6074c5d9d583881baf599719c4baf5197000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000064c0b60900000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000d9b3138da7fea1a58e9abcaca4f9157f000000000000000000000000b4d133c96d8833ea8a2275a1c0ce6d2204661624000000000000000000000000789e35a999c443fe6089544056f728239b8ffee7d18dc56a3bc9d77a752446e5a877c253de788975fb7dee4026e99f4800b56a7a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000064b8cbd80000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000026ce37b6469a8451b89efac527566cda00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000016000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004860000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000003ee20e6486400000000000000000000000000000000000000000000000000000000000000004860000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000021c70000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000003ff2e795f5000000000000000000000000000000000000000000000000000000000000000021c70000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000140f3db838ab4dd473a8912cceebd42c09a435a91f3170af37d4f011b5c2c070a00000000000000000000000000000000000000000000000000000000000000820bb9c08334a845e5f055d54d2bb3ab1727b520db54c32c0c2e5c1cfc4fd040033bd3440d4c327e9705d5d4d15d45b274aca2aaff777b5682789950743d3c91e21bafe4d68309dddf8d4e1802926976669bf45db32275b02e13854723b4c814a2384976973926fe75f68fb31cc77d70429b68c21dd91e8e4cc564cc799029ba7e4d1c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000059e8124421aee0d8ae2a627b54da6e026250bee9b3df77b111d8e39c9c0a91ea06038a4834d6a27feaa91f88e2208bcb07f3fdde45345c77450a07c9f4515970b21b010e78ce6af68e5d010513ff70a3aaed9afeb8661116e6ce00000000000000332d1229"
    )
}
