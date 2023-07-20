package com.rarible.protocol.order.listener.service.descriptors.exchange.blur

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.listener.data.log
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigDecimal

class BlurV2Execution721TakerFeePackedDescriptorTest : AbstractBlurV2ExecutionDescriptorTest() {
    private val descriptor = BlurV2kpExecution721TakerFeePackedDescriptor(
        contractsProvider = contractsProvider,
        blurV2EventConverter = blurV2EventConverter,
    )

    @Test
    fun `convert simple hash`() = runBlocking<Unit> {
        // https://etherscan.io/tx/0x9858aed50bafc8f95241730d1a43b66fffb5d8e9b95a2a89917d74f73ce65771#eventlog
        val log = log(
            topics = listOf(
                Word.apply("0x0fcf17fac114131b10f37b183c6a60f905911e52802caeeb3e6ea210398b81ab"),
            ),
            data = "0x00ac3680a1ab578def3e4473c47ed80aa51b9bc3046cf80585a56f9a485f8e890000000000000000000dff001e863363f0aea30d45bc4224783b48cdd1f3d90d010000000b1a2bc2ec5000006339e5e072086621540d0362c4e3cea0d643e114000000000000000000000032c8f8e2f59dd95ff67c3d39109eca2e2a017d4c8a"
        )
        val transient = mockkTransaction(
            input = execution721TakerFeePackedTx,
            from = Address.apply("0x1c4351C97177418cEC07FF733f107fb9879dBeD1")
        )
        val ethereumBlockchainLog = EthereumBlockchainLog(
            ethLog = log,
            ethTransaction = transient,
            index = 0,
            total = 1
        )
        val expectedNft = Asset(
            Erc721AssetType(
                Address.apply("0x6339e5e072086621540d0362c4e3cea0d643e114"),
                EthUInt256.of(3583)
            ),
            EthUInt256.ONE
        )
        val expectedPayment = Asset(
            Erc20AssetType(contractsProvider.weth()),
            EthUInt256.of("800000000000000000")
        )

        val matches = descriptor.convert<OrderSideMatch>(mockkBlock, ethereumBlockchainLog)
        assertThat(matches).hasSize(2)

        val left = matches.single { it.side == OrderSide.LEFT }
        assertThat(left.hash)
            .isEqualTo(Word.apply("0x00ac3680a1ab578def3e4473c47ed80aa51b9bc3046cf80585a56f9a485f8e89"))
        assertThat(left.maker).isEqualTo(Address.apply("0x1e863363F0aeA30D45bc4224783B48cDd1f3d90d"))
        assertThat(left.taker).isEqualTo(Address.apply("0x1c4351C97177418cEC07FF733f107fb9879dBeD1"))
        assertThat(left.make).isEqualTo(expectedPayment)
        assertThat(left.take).isEqualTo(expectedNft)
        assertThat(left.fill).isEqualTo(expectedPayment.value)
        assertThat(left.date).isEqualTo(blockTimestamp)
        assertThat(left.takeValue).isEqualTo(BigDecimal.ONE)
        assertThat(left.makeValue).isEqualTo(BigDecimal("0.800000000000000000"))
        assertThat(left.source).isEqualTo(HistorySource.BLUR)
        assertThat(left.adhoc).isFalse
        assertThat(left.counterAdhoc).isTrue
        assertThat(left.originFees).isEmpty()

        val right = matches.single { it.side == OrderSide.RIGHT }
        assertThat(right.counterHash)
            .isEqualTo(Word.apply("0x00ac3680a1ab578def3e4473c47ed80aa51b9bc3046cf80585a56f9a485f8e89"))
        assertThat(right.maker).isEqualTo(Address.apply("0x1c4351C97177418cEC07FF733f107fb9879dBeD1"))
        assertThat(right.taker).isEqualTo(Address.apply("0x1e863363F0aeA30D45bc4224783B48cDd1f3d90d"))
        assertThat(right.make).isEqualTo(expectedNft)
        assertThat(right.take).isEqualTo(expectedPayment)
        assertThat(right.fill).isEqualTo(expectedNft.value)
        assertThat(right.date).isEqualTo(blockTimestamp)
        assertThat(right.makeValue).isEqualTo(BigDecimal.ONE)
        assertThat(right.takeValue).isEqualTo(BigDecimal("0.800000000000000000"))
        assertThat(right.source).isEqualTo(HistorySource.BLUR)
        assertThat(right.adhoc).isTrue
        assertThat(right.counterAdhoc).isFalse
        assertThat(right.originFees?.single()?.account)
            .isEqualTo(Address.apply("0xc8f8e2f59dd95ff67c3d39109eca2e2a017d4c8a"))
        assertThat(right.originFees?.single()?.value)
            .isEqualTo(EthUInt256.of(50))
    }

    private val execution721TakerFeePackedTx = Binary.apply(
        "0xda815cb5000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000003800000000000000000000000001e863363f0aea30d45bc4224783b48cdd1f3d90d0000000000000000000000006339e5e072086621540d0362c4e3cea0d643e114cc998b69290ff91d344941e8207560e63f38af7d9b41ff6c650e911eb725b1200000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000006697809d000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001384ee45b824d6b7364c7d9555016a2c00000000000000000000000000000000000000000000000000000000000001a0000000000000000000000000c8f8e2f59dd95ff67c3d39109eca2e2a017d4c8a000000000000000000000000000000000000000000000000000000000000003200000000000000000000000000000000000000000000000000000000000002c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000000000000000000000000b1a2bc2ec5000000000000000000000000000000000000000000000000000000000000000000dff000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000041fbf214c90b06a27a7ebe68f49d6b6e5b5eafc2e5fc248aa53b5392bff9d55bd01e2534a55beff360c838d1d38e56a00995e22a713a6081cd7a2b4dd62e807e811c0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000599729358d37d96b36e1c4e17d97a13e6f1d4bdfb5afa44cc5b228582e33a07e4255c1f96ee8e1080b1d99ce50043f7635a18fbf79f0300c50ba81353dcb458eff1b010e619b6af68e5d010513ff70a3aaed9afeb8661116e6ce00000000000000"
    )

}