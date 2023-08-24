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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal

class BlurV2Execution721MakerFeePackedDescriptorTest : AbstractBlurV2ExecutionDescriptorTest() {
    private val descriptor = BlurV2Execution721MakerFeePackedDescriptor(
        contractsProvider = contractsProvider,
        blurV2EventConverter = blurV2EventConverter,
    )

    @Test
    fun `convert single sell`() = runBlocking<Unit> {
        // https://etherscan.io/tx/0x6e3baa0eedd5b4e14d2ffd751afee23fc64066b89e6eb5725a12fd0e9e334331#eventlog^
        val log = log(
            topics = listOf(
                Word.apply("0x7dc5c0699ac8dd5250cbe368a2fc3b4a2daadb120ad07f6cccea29f83482686e"),
            ),
            data = "0x5d5a001f37e18c020eddbd9e25fd98dabfe3f0a3d263b841dd2a7891f9eb6f250000000000000000000ae70027aced470e1e232babf2da93a78269bb5739c48c00000000061b31ab352c00006c952af158ec8d0fd94908e389c084394d9aebbb00000000000000000000003229ffea86733d7feac7c353343f300e99b8910c77"
        )
        val transient = mockkTransaction(execution721MakerFeePackedTx)
        val ethereumBlockchainLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transient,
            index = 0,
            total = 1
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x6c952af158ec8d0fd94908e389c084394d9aebbb"),
                EthUInt256.of(2791)
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            EthAssetType,
            EthUInt256.of("440000000000000000")
        )

        val bidOrderUsd = randomBidOrderUsdValue()
        val sellOrderUsd = randomSellOrderUsdValue()

        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), at = any()) } returns sellOrderUsd
        coEvery { priceUpdateService.getAssetsUsdValue(any(), any(), at = any()) } returns bidOrderUsd

        val matches = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog)
        assertThat(matches).hasSize(2)

        val left = matches.single { it.side == OrderSide.LEFT }
        assertThat(left.hash).isEqualTo(Word.apply("0x5d5a001f37e18c020eddbd9e25fd98dabfe3f0a3d263b841dd2a7891f9eb6f25"))
        assertThat(left.maker).isEqualTo(Address.apply("0x27aced470e1e232babf2da93a78269bb5739c48c"))
        assertThat(left.taker).isEqualTo(Address.apply("0x3ab15e75203dfce15c450a6cc1b205a33d40e7ea"))
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.make).isEqualTo(expectedNft)
        assertThat(left.take).isEqualTo(expectedPayment)
        assertThat(left.fill).isEqualTo(expectedNft.value)
        assertThat(left.date).isEqualTo(blockTimestamp)
        assertThat(left.takeValue).isEqualTo(BigDecimal("0.440000000000000000"))
        assertThat(left.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.source).isEqualTo(HistorySource.BLUR)
        assertThat(left.adhoc).isFalse
        assertThat(left.counterAdhoc).isTrue
        assertThat(left.originFees?.single()?.account).isEqualTo(Address.apply("0x29ffea86733d7feac7c353343f300e99b8910c77"))
        assertThat(left.originFees?.single()?.value).isEqualTo(EthUInt256.of("50"))

        val right = matches.single { it.side == OrderSide.RIGHT }
        assertThat(right.counterHash).isEqualTo(Word.apply("0x5d5a001f37e18c020eddbd9e25fd98dabfe3f0a3d263b841dd2a7891f9eb6f25"))
        assertThat(right.maker).isEqualTo(Address.apply("0x3ab15e75203dfce15c450a6cc1b205a33d40e7ea"))
        assertThat(right.taker).isEqualTo(Address.apply("0x27aced470e1e232babf2da93a78269bb5739c48c"))
        assertThat(right.make).isEqualTo(expectedPayment)
        assertThat(right.take).isEqualTo(expectedNft)
        assertThat(right.fill).isEqualTo(expectedPayment.value)
        assertThat(right.date).isEqualTo(blockTimestamp)
        assertThat(right.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.makeValue).isEqualTo(BigDecimal("0.440000000000000000"))
        assertThat(right.source).isEqualTo(HistorySource.BLUR)
        assertThat(right.adhoc).isTrue
        assertThat(right.counterAdhoc).isFalse
        assertThat(right.originFees).isEmpty()
    }

    private val execution721MakerFeePackedTx = Binary.apply(
        "0x70bce2d6000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000003a000000000000000000000000027aced470e1e232babf2da93a78269bb5739c48c0000000000000000000000006c952af158ec8d0fd94908e389c084394d9aebbb06bfe75dd7682abe4d1269732051a59f5b08da9d6a1b01b00be13469b3743a5300000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000064bf25db000000000000000000000000000000000000000000000000000000000000000000000000000000000000000029ffea86733d7feac7c353343f300e99b8910c77000000000000000000000000000000000000000000000000000000000000003200000000000000000000000000000000ec86a2128702af671a9d161a353f4fe200000000000000000000000000000000000000000000000000000000000001c00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002e00000000000000000000000003ab15e75203dfce15c450a6cc1b205a33d40e7ea0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000ae70000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000061b31ab352c00000000000000000000000000000000000000000000000000000000000000000ae700000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004178d19517b3aa3110e1670f229b09a1cddfc062cfa2ccf26ede9dc6d482f1eb8f55d63c68bc770a854674ff99f6d2ebbf9cfdb1b5aee22addd84bcc8ba43ccfa21b00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000005906819a12e32f9a21b83012e2712ea5479086032124575f620397f95e2c42ec9271816d23d1208598306675c55272926cac14d15fc7d8e5fd653a465eb1c2a5e31c010e61b46af68e5d010513ff70a3aaed9afeb8661116e6ce00000000000000332d1229"
    )
}
